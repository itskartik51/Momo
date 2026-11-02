const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendMomoPushNotification = functions.database
  .ref("/momo_chat/messages/{receiverCode}/{messageId}")
  .onCreate(async (snapshot, context) => {
    const receiverCode = context.params.receiverCode;
    const messageId = context.params.messageId;
    const data = snapshot.val();

    if (!data) return null;

    try {
      const tokenSnapshot = await admin
        .database()
        .ref(`/momo_chat/fcm_tokens/${receiverCode}`)
        .once("value");

      const fcmToken = tokenSnapshot.val();

      if (!fcmToken || typeof fcmToken !== "string") {
        console.log(`No valid FCM token registered for receiver ${receiverCode}`);
        return null;
      }

      const payload = {
        token: fcmToken,
        data: {
          id: String(data.id || messageId),
          send: String(data.send || ""),
          text: String(data.text || ""),
          time: String(data.time || Date.now()),
          mType: String(data.mType || "0"),
          fId: String(data.fId || ""),
          cKey: String(data.cKey || ""),
          cIv: String(data.cIv || ""),
          fSize: String(data.fSize || "0"),
          bPrev: String(data.bPrev || ""),
          rId: String(data.rId || ""),
          rText: String(data.rText || ""),
          rSend: String(data.rSend || "")
        },
        android: {
          priority: "high"
        }
      };

      const response = await admin.messaging().send(payload);
      console.log("FCM notification dispatched successfully:", response);
      return response;
    } catch (error) {
      console.error("Error sending FCM notification:", error);
      return null;
    }
  });
