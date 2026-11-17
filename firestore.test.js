const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kmobile_1";
const BOB_UID = "bob_kmobile_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validContractPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    contractNumber: "KM-2026-4821",
    fullName: "Иван Петров",
    documentIdNumber: "4510 123456",
    contactEmail: "ivan@example.com",
    virtualNumber: "+79901234567",
    tariffId: "free_0",
    tariffPriceRub: 0,
    status: "ACTIVE",
    signatureStrokeCount: 3,
    signaturePointsJson: "10,20;30,40",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validCallPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    callerVirtualNumber: "+79901234567",
    recipientNumber: "+79995551122",
    recipientName: "Анна",
    networkType: "Wi-Fi",
    tariffId: "free_0",
    callMode: "INTERNET_TO_REAL",
    status: "INITIATED",
    createdAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write contracts or calls", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("contracts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contracts")
      .doc("primary")
      .set(validContractPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("calls").get()
  );
});

test("Authenticated owner can create contract with +7990, switch tariff to 100, and terminate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const contractRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("contracts")
    .doc("primary");

  await assertSucceeds(contractRef.set(validContractPayload(ALICE_UID)));

  // Switch tariff from free_0 (0) to pro_100 (100)
  await assertSucceeds(
    contractRef.update({
      tariffId: "pro_100",
      tariffPriceRub: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminate active contract
  await assertSucceeds(
    contractRef.update({
      status: "TERMINATED",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Contract validation blocks non-+7990 numbers, mismatched tariff prices, ghost fields, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const contractsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("contracts");

  // Non-+7990 number rejected
  await assertFails(
    contractsCol
      .doc("bad_num")
      .set(validContractPayload(ALICE_UID, { virtualNumber: "+79991234567" }))
  );

  // Mismatched tariff price rejected (pro_100 cannot cost 0)
  await assertFails(
    contractsCol
      .doc("bad_price")
      .set(
        validContractPayload(ALICE_UID, {
          tariffId: "pro_100",
          tariffPriceRub: 0,
        })
      )
  );

  // Ghost field / @DocumentId 'id' field rejected
  await assertFails(
    contractsCol
      .doc("ghost")
      .set(validContractPayload(ALICE_UID, { id: "ghost", isAdmin: true }))
  );

  // Future timestamp rejected
  await assertFails(
    contractsCol.doc("future").set(
      validContractPayload(ALICE_UID, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice contract and calls", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contracts")
      .doc("primary")
      .set(validContractPayload(ALICE_UID));
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("calls")
      .doc("call_1")
      .set(validCallPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contracts")
      .doc("primary")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contracts")
      .doc("primary")
      .update({ status: "TERMINATED" })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("calls").get()
  );
});

test("Authenticated owner can create and list call records with +7990 caller ID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const callsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("calls");

  await assertSucceeds(callsCol.doc("call_1").set(validCallPayload(ALICE_UID)));
  await assertSucceeds(callsCol.orderBy("createdAt", "desc").get());
  await assertSucceeds(callsCol.doc("call_1").delete());
});
