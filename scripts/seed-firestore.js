// Seeds fake leaderboard entries at the exact Firestore paths
// DefaultFirestorePathStrategy builds (see leaderboard/data's FirestorePathStrategy.kt):
//   leaderboards/{boardId}/windows/{windowBucket}/entries/{userId}
//   leaderboards/{boardId}/windows/{windowBucket}/friendsOf/{currentUserId}/entries/{userId}
//
// Usage:
//   npm install
//   node seed-firestore.js <your-current-signed-in-uid>
//
// <your-current-signed-in-uid> is the anonymous Firebase Auth uid the sample
// app is currently signed in as (see it in Logcat, in any Firestore query
// path line — same one SampleUser.ID resolves to on-device). Needed only for
// the friends_demo board, whose entries live under .../friendsOf/{that-uid}/entries.
//
// Credentials: if scripts/serviceAccountKey.json exists (Firebase console ->
// Project settings -> Service accounts -> Generate new private key), that's
// used. Otherwise falls back to Application Default Credentials — the
// simpler option when running in Cloud Shell, which is already authenticated
// as your Google account and needs no key file at all (as long as that
// account has write access to the Firebase project, e.g. as its owner).

const { initializeApp, cert, applicationDefault } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const fs = require('fs');
const path = require('path');

const currentUserId = process.argv[2];
if (!currentUserId) {
  console.error('Usage: node seed-firestore.js <your-current-signed-in-uid>');
  console.error('(find it in Logcat — any Firestore query path contains .../entries/<uid>)');
  process.exit(1);
}

const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');
const credential = fs.existsSync(serviceAccountPath)
  ? cert(require(serviceAccountPath))
  : applicationDefault();

const app = initializeApp({ credential });
const db = getFirestore(app);

// Matches FIELD_USER_ID/FIELD_DISPLAY_NAME/... in FirestoreLeaderboardEntryMapper.kt.
function entryDoc({ userId, displayName, avatarId, score }) {
  return { userId, displayName, avatarId, score, rank: null, metadata: {} };
}

const FAKE_USERS = [
  { userId: 'demo_user_2', displayName: 'Jordan Lee', avatarId: 'avatar_02', score: 8435 },
  { userId: 'demo_user_3', displayName: 'Sam Patel', avatarId: 'avatar_03', score: 6210 },
  { userId: 'demo_user_5', displayName: 'Morgan Diaz', avatarId: 'avatar_05', score: 5642 },
  { userId: 'demo_user_6', displayName: 'Taylor Chen', avatarId: 'avatar_06', score: 5278 },
  { userId: 'demo_user_9', displayName: 'Drew Sato', avatarId: 'avatar_09', score: 3069 },
  { userId: 'demo_user_10', displayName: 'Avery Novak', avatarId: 'avatar_10', score: 2514 },
  { userId: 'demo_user_11', displayName: 'Quinn Osei', avatarId: 'avatar_11', score: 1494 },
  { userId: 'demo_user_12', displayName: 'Reese Park', avatarId: 'avatar_12', score: 7302 },
  { userId: 'demo_user_13', displayName: 'Harper Nguyen', avatarId: 'avatar_01', score: 6890 },
  { userId: 'demo_user_14', displayName: 'Rowan Silva', avatarId: 'avatar_04', score: 4715 },
  { userId: 'demo_user_15', displayName: 'Emerson Cole', avatarId: 'avatar_07', score: 4033 },
  { userId: 'demo_user_16', displayName: 'Finley Adeyemi', avatarId: 'avatar_08', score: 3588 },
  { userId: 'demo_user_17', displayName: 'Marlowe Tran', avatarId: 'avatar_02', score: 2246 },
  { userId: 'demo_user_18', displayName: 'Sasha Volkov', avatarId: 'avatar_09', score: 1877 },
  { userId: 'demo_user_19', displayName: 'Devon Okafor', avatarId: 'avatar_06', score: 980 },
];

// SampleUser.FRIEND_IDS in the sample app — must match exactly for
// LeaderboardScope.Friends(currentUserId, FRIEND_IDS) to resolve these.
const FAKE_FRIENDS = [
  { userId: 'friend_01', displayName: 'Casey Kim', avatarId: 'avatar_07', score: 4120 },
  { userId: 'friend_02', displayName: 'Riley Fox', avatarId: 'avatar_08', score: 3350 },
  { userId: 'friend_03', displayName: 'Jamie Ortiz', avatarId: 'avatar_01', score: 2870 },
  { userId: 'friend_04', displayName: 'Skyler Wren', avatarId: 'avatar_12', score: 1990 },
];

// Mirrors TimeWindowBucket.currentBucketId's TimeWindow.Weekly branch: "wk-"
// plus the Monday (UTC) of the current week, as an ISO date.
function currentWeeklyBucket(now = new Date()) {
  const epochDay = Math.floor(now.getTime() / 86_400_000);
  const daysSinceMonday = (((epochDay + 3) % 7) + 7) % 7;
  const monday = new Date((epochDay - daysSinceMonday) * 86_400_000);
  const y = monday.getUTCFullYear();
  const m = String(monday.getUTCMonth() + 1).padStart(2, '0');
  const d = String(monday.getUTCDate()).padStart(2, '0');
  return `wk-${y}-${m}-${d}`;
}

// Mirrors TimeWindowBucket.currentBucketId's TimeWindow.Monthly branch:
// "%04d-%02d" of the current UTC year/month.
function currentMonthlyBucket(now = new Date()) {
  const y = now.getUTCFullYear();
  const m = String(now.getUTCMonth() + 1).padStart(2, '0');
  return `${y}-${m}`;
}

async function seedEntries(collectionPath, users) {
  const batch = db.batch();
  for (const user of users) {
    batch.set(db.collection(collectionPath).doc(user.userId), entryDoc(user));
  }
  await batch.commit();
  console.log(`Seeded ${users.length} entries at ${collectionPath}`);
}

async function main() {
  // AllTime board: bucket is always "all" (TimeWindowBucket.kt). CustomThemeBoardDemo
  // and CustomRowBoardDemo both reuse this same board — they only change rendering
  // (theme/rowContent), not the data source — so there's nothing separate to seed for them.
  await seedEntries('leaderboards/global_alltime/windows/all/entries', FAKE_USERS);

  // Weekly board: bucket rotates, so this only stays valid for the current week.
  const weeklyBucket = currentWeeklyBucket();
  await seedEntries(`leaderboards/weekly_demo/windows/${weeklyBucket}/entries`, FAKE_USERS);

  // Monthly board: bucket rotates, so this only stays valid for the current month.
  const monthlyBucket = currentMonthlyBucket();
  await seedEntries(`leaderboards/monthly_demo/windows/${monthlyBucket}/entries`, FAKE_USERS);

  // Friends board: scoped under the signed-in user's own friendsOf subcollection.
  await seedEntries(
    `leaderboards/friends_demo/windows/all/friendsOf/${currentUserId}/entries`,
    FAKE_FRIENDS,
  );

  console.log('Done.');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
