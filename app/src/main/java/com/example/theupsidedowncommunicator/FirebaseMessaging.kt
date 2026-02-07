package com.example.theupsidedowncommunicator

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestoreException

data class MessageRecord(
    val from: String = "",
    val content: String = "",
    val mode: String = "MORSE",
    val timestamp: Long = 0
)

object FirebaseMessaging {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val broadcastCollection by lazy { db.collection("broadcast") }

    /**
     * Sends a message to the Firestore "broadcast" collection.
     */
    fun sendBroadcast(fromUserId: String, content: String, mode: EncodeMode) {
        val message = hashMapOf(
            "from" to fromUserId,
            "content" to content,
            "mode" to mode.name,
            "timestamp" to Timestamp.now()
        )
        
        broadcastCollection.add(message)
    }

    /**
     * Listens to the Firestore "broadcast" collection for real-time updates.
     */
    fun fetchBroadcasts(onUpdate: (List<MessageRecord>) -> Unit) {
        broadcastCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val list = snapshot.documents.mapNotNull { doc ->
                    val from = doc.getString("from") ?: ""
                    val content = doc.getString("content") ?: ""
                    val mode = doc.getString("mode") ?: "MORSE"
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    
                    MessageRecord(from, content, mode, timestamp)
                }
                onUpdate(list)
            }
    }
}
