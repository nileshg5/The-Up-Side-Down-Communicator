package com.example.theupsidedowncommunicator

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class MessageRecord(
    val from: String = "",
    val content: String = "",
    val mode: String = "MORSE",
    val timestamp: Long = 0
)

object FirebaseMessaging {
    // 1. Reference to the Realtime Database (the URL in your screenshot)
    private val database = FirebaseDatabase.getInstance().reference
    private val broadcastRef = database.child("broadcasts")
    private val privateRef = database.child("private_messages")

    /**
     * Sends a message to the GLOBAL broadcast feed.
     */
    fun sendBroadcast(fromUserId: String, content: String, mode: EncodeMode) {
        val message = MessageRecord(
            from = fromUserId,
            content = content,
            mode = mode.name,
            timestamp = System.currentTimeMillis()
        )
        // push() creates a unique ID so messages don't overwrite each other
        broadcastRef.push().setValue(message)
    }

    /**
     * Sends a PRIVATE message to a specific frequency (Target ID).
     */
    fun sendPrivateMessage(targetId: String, fromUserId: String, content: String, mode: EncodeMode) {
        val message = MessageRecord(
            from = fromUserId,
            content = content,
            mode = mode.name,
            timestamp = System.currentTimeMillis()
        )
        // This saves it directly under the ID (e.g., private_messages/1234)
        privateRef.child(targetId).setValue(message)
    }

    /**
     * Listens for all broadcast messages in real-time.
     */
    fun fetchBroadcasts(onUpdate: (List<MessageRecord>) -> Unit) {
        broadcastRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull {
                    it.getValue(MessageRecord::class.java)
                }.sortedByDescending { it.timestamp }

                onUpdate(list)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database errors here
            }
        })
    }

    /**
     * Listens for private messages sent to YOUR ID.
     */
    fun listenForPrivateMessages(myId: String, onMessageReceived: (MessageRecord) -> Unit) {
        privateRef.child(myId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(MessageRecord::class.java)
                if (msg != null) {
                    onMessageReceived(msg)
                    // Optional: clear message after receiving
                    // privateRef.child(myId).removeValue()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}