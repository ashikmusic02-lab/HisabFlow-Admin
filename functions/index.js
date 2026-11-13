const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Replace with your exact owner Gmail address before deploying.
const ADMIN_EMAIL = "REPLACE_WITH_MY_GMAIL";
const ALLOWED_TARGETS = new Set(["all", "tester"]);
const ALLOWED_SCREENS = new Set(["", "home", "reports", "budgets", "goals", "settings"]);
const TTL_24_HOURS_MS = 24 * 60 * 60 * 1000;

exports.sendPush = onCall({ region: "us-central1" }, async (request) => {
  if (
    !request.auth ||
    !request.auth.token ||
    request.auth.token.email !== ADMIN_EMAIL ||
    request.auth.token.email_verified !== true
  ) {
    throw new HttpsError(
      "permission-denied",
      "Only the verified admin account can send notifications."
    );
  }

  const data = request.data || {};
  const title = typeof data.title === "string" ? data.title.trim() : "";
  const body = typeof data.body === "string" ? data.body.trim() : "";
  const imageUrl = typeof data.imageUrl === "string" ? data.imageUrl.trim() : "";
  const screen = typeof data.screen === "string" ? data.screen.trim() : "";
  const target = typeof data.target === "string" ? data.target.trim() : "";

  if (!title || title.length > 60) {
    throw new HttpsError(
      "invalid-argument",
      "Title is required and must be at most 60 characters."
    );
  }

  if (!body || body.length > 200) {
    throw new HttpsError(
      "invalid-argument",
      "Message body is required and must be at most 200 characters."
    );
  }

  if (imageUrl && !imageUrl.startsWith("https://")) {
    throw new HttpsError(
      "invalid-argument",
      "imageUrl must start with https:// when provided."
    );
  }

  if (!ALLOWED_SCREENS.has(screen)) {
    throw new HttpsError("invalid-argument", "Invalid target screen.");
  }

  if (!ALLOWED_TARGETS.has(target)) {
    throw new HttpsError(
      "invalid-argument",
      "Target topic must be either 'all' or 'tester'."
    );
  }

  // Send a DATA-ONLY FCM message (no 'notification' block)
  const message = {
    topic: target,
    data: {
      title,
      body,
      imageUrl: imageUrl || "",
      screen: screen || "",
    },
    android: {
      priority: "high",
      ttl: TTL_24_HOURS_MS,
    },
  };

  const messageId = await getMessaging().send(message);

  await getFirestore().collection("push_history").add({
    title,
    body,
    imageUrl: imageUrl || "",
    screen: screen || "",
    target,
    timestamp: FieldValue.serverTimestamp(),
  });

  return { success: true, messageId };
});
