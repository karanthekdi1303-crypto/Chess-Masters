import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: process.env.FIREBASE_API_KEY,
  projectId: process.env.FIREBASE_PROJECT_ID,
  // Additional configuration can be added if needed, like authDomain, storageBucket, etc.
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Enable Authentication and Firestore modules
export const auth = getAuth(app);
export const db = getFirestore(app);
