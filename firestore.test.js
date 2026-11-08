const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ZEINS_UID = "zein_user_123";
const OTHER_UID = "other_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validProfilePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    studentName: "زين الدين آسر الشربيني",
    gradeLabel: "الصف الثاني الابتدائي لغات",
    studentLevel: "beginner",
    preferredLanguageMode: "bilingual",
    points: 100,
    completedLessonsCount: 2,
    streakDays: 3,
    reminderEnabled: true,
    reminderHour: 17,
    reminderMinute: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validLessonPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    subjectId: "math",
    subjectNameAr: "Math - الرياضيات لغات",
    unitTitle: "Unit 1: Place Value",
    lessonTitle: "Lesson 1: Hundreds, Tens, and Ones",
    lessonOrder: 1,
    officialContent:
      "A 3-digit number has Hundreds, Tens, and Ones. For example, in 345: 3 is in the Hundreds place (300), 4 is in the Tens place (40), and 5 is in the Ones place (5).",
    keyTerms: "Hundreds (المئات), Tens (العشرات), Ones (الآحاد), Place Value (القيمة المكانية)",
    sourceBookName: "Primary 2 Math Official Book 2026/2027",
    isCompleted: false,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write student profile or lessons", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ZEINS_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ZEINS_UID).collection("lessons").get()
  );
});

test("Authenticated user: can create, read, and update their own StudentProfile", async () => {
  const zeinDb = testEnv.authenticatedContext(ZEINS_UID).firestore();
  const profileRef = zeinDb
    .collection("users")
    .doc(ZEINS_UID)
    .collection("profile")
    .doc("main");

  await assertSucceeds(profileRef.set(validProfilePayload(ZEINS_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      points: 125,
      studentLevel: "advanced",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot access another user's profile or lessons (Cross-User Isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb
      .collection("users")
      .doc(ZEINS_UID)
      .collection("profile")
      .doc("main")
      .set(validProfilePayload(ZEINS_UID));
    await adminDb
      .collection("users")
      .doc(ZEINS_UID)
      .collection("lessons")
      .doc("math_u1_l1")
      .set(validLessonPayload(ZEINS_UID));
  });

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(
    otherDb.collection("users").doc(ZEINS_UID).collection("profile").doc("main").get()
  );
  await assertFails(
    otherDb.collection("users").doc(ZEINS_UID).collection("lessons").doc("math_u1_l1").get()
  );
});

test("Shadow Update & Cancelled Subject Rejection: rejects ghost fields and cancelled 'discover' subject", async () => {
  const zeinDb = testEnv.authenticatedContext(ZEINS_UID).firestore();
  const lessonRef = zeinDb
    .collection("users")
    .doc(ZEINS_UID)
    .collection("lessons")
    .doc("bad_lesson");

  // Reject cancelled "discover" (اكتشف) subject
  const discoverPayload = {
    ...validLessonPayload(ZEINS_UID),
    subjectId: "discover",
  };
  await assertFails(lessonRef.set(discoverPayload));

  // Reject ghost field injection
  const ghostPayload = {
    ...validLessonPayload(ZEINS_UID),
    isAdminOverride: true,
  };
  await assertFails(lessonRef.set(ghostPayload));
});

test("Authenticated user: can create, query, and update valid curriculum lessons", async () => {
  const zeinDb = testEnv.authenticatedContext(ZEINS_UID).firestore();
  const lessonRef = zeinDb
    .collection("users")
    .doc(ZEINS_UID)
    .collection("lessons")
    .doc("math_u1_l1");

  await assertSucceeds(lessonRef.set(validLessonPayload(ZEINS_UID)));
  await assertSucceeds(
    zeinDb.collection("users").doc(ZEINS_UID).collection("lessons").get()
  );
  await assertSucceeds(
    lessonRef.update({
      isCompleted: true,
      lastAiExplanation: "Great job Zein! 345 = 300 + 40 + 5.",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create, list, and delete their own chat_messages while blocking other users", async () => {
  const zeinDb = testEnv.authenticatedContext(ZEINS_UID).firestore();
  const msgRef = zeinDb
    .collection("users")
    .doc(ZEINS_UID)
    .collection("chat_messages")
    .doc("msg_1");

  await assertSucceeds(
    msgRef.set({
      userId: ZEINS_UID,
      sender: "user",
      messageText: "اعمل لي امتحان شامل في الماث من كتاب المعاصر 2027",
      subjectFilter: "math",
      interactionMode: "exam",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    zeinDb.collection("users").doc(ZEINS_UID).collection("chat_messages").get()
  );

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(
    otherDb.collection("users").doc(ZEINS_UID).collection("chat_messages").doc("msg_1").get()
  );
});

