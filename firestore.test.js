const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read or write user devices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("devices").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("devices").doc("dev1").set({
    deviceName: "Test",
    status: "online",
    batteryLevel: 90,
    complianceStatus: "compliant"
  }));
});

test("Authenticated user: cannot read another user's devices", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("devices").doc("bob_phone").set({
      deviceName: "Bob Phone",
      status: "online",
      batteryLevel: 80,
      complianceStatus: "compliant",
      createdAt: new Date(),
      updatedAt: new Date()
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("devices").doc("bob_phone").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("devices").get());
});

test("Authenticated user: can create, read, update, and delete own devices", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const devDoc = aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("alice_tablet");

  // Create
  await assertSucceeds(devDoc.set({
    deviceName: "Alice Work Tablet",
    model: "Pixel Tablet",
    status: "online",
    batteryLevel: 85,
    cpuUsage: 25,
    ramUsageMb: 2048,
    ramTotalMb: 8192,
    complianceStatus: "compliant",
    lastLatitude: 37.7749,
    lastLongitude: -122.4194,
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  // Read
  await assertSucceeds(devDoc.get());

  // Update
  await assertSucceeds(devDoc.update({
    batteryLevel: 75,
    cpuUsage: 35,
    updatedAt: new Date()
  }));

  // Delete
  await assertSucceeds(devDoc.delete());
});

test("Device validation: fails if batteryLevel is out of bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const devDoc = aliceDb.collection("users").doc(ALICE_UID).collection("devices").doc("bad_dev");

  await assertFails(devDoc.set({
    deviceName: "Bad Device",
    status: "online",
    batteryLevel: 150, // Invalid: > 100
    complianceStatus: "compliant",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Security logs: can create and read, but cannot update (immutable)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logDoc = aliceDb.collection("users").doc(ALICE_UID).collection("security_logs").doc("log_1");

  await assertSucceeds(logDoc.set({
    deviceId: "dev_1",
    deviceName: "Pixel 7",
    severity: "critical",
    eventType: "restricted-app-usage",
    description: "Policy violation detected on endpoint",
    createdAt: new Date()
  }));

  // Updates are forbidden
  await assertFails(logDoc.update({
    description: "Attempted tampering"
  }));
});

test("Camera snapshots: user can create, read/list, and delete own snapshots", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const snapDoc = aliceDb.collection("users").doc(ALICE_UID).collection("camera_snapshots").doc("snap_1");

  await assertSucceeds(snapDoc.set({
    deviceId: "dev_1",
    deviceName: "Pixel 7 Pro",
    timestamp: Date.now(),
    cameraFacing: "back",
    resolution: "1920x1080",
    triggerReason: "Remote surveillance trigger",
    storageUrl: "",
    base64Data: "data:image/jpeg;base64,...",
    torchActive: false,
    notes: "Live optical verification",
    createdAt: new Date()
  }));

  // List / get
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("camera_snapshots").get());
  await assertSucceeds(snapDoc.get());

  // Delete
  await assertSucceeds(snapDoc.delete());

  // Other user cannot read Alice's snapshot
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("camera_snapshots").get());
});

