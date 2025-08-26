package com.messenger.sample.desktop.ui

import uniffi.mls_rs_uniffi.CipherSuite
import uniffi.mls_rs_uniffi.Client
import uniffi.mls_rs_uniffi.ClientConfig
import uniffi.mls_rs_uniffi.EpochRecord
import uniffi.mls_rs_uniffi.Group
import uniffi.mls_rs_uniffi.GroupStateStorage
import uniffi.mls_rs_uniffi.Message
import uniffi.mls_rs_uniffi.ReceivedMessage
import uniffi.mls_rs_uniffi.generateSignatureKeypair

internal class InMemoryGroupStateStorage(
    private val id: String = ""
) : GroupStateStorage {
    private val groupStates = mutableMapOf<ByteArray, ByteArray>()
    private val epochRecords = mutableMapOf<ByteArray, MutableMap<ULong, ByteArray>>()

    override fun write(
        groupId: ByteArray,
        groupState: ByteArray,
        epochInserts: List<EpochRecord>,
        epochUpdates: List<EpochRecord>
    ) {
        groupStates[groupId] = groupState
        println("InMemoryGroupStateStorage-$id write $groupId")
        // Store epoch records
        val epochs = epochRecords.getOrPut(groupId) { mutableMapOf() }
        for (record in epochInserts) {
            epochs[record.id] = record.data
        }
        for (record in epochUpdates) {
            epochs[record.id] = record.data
        }
    }

    override fun maxEpochId(groupId: ByteArray): ULong? {
        println("InMemoryGroupStateStorage-$id maxEpochId $groupId")
        return epochRecords[groupId]?.keys?.maxOrNull()
    }

    override fun epoch(groupId: ByteArray, epochId: ULong): ByteArray? {
        println("InMemoryGroupStateStorage-$id epoch $groupId")
        return epochRecords[groupId]?.get(epochId)
    }

    override fun state(groupId: ByteArray): ByteArray? {
        println("InMemoryGroupStateStorage-$id state $groupId")
        return groupStates[groupId]
    }
}


fun createClient(userId: String, storageId: String = ""): Client {
    val storage = InMemoryGroupStateStorage(storageId)
    val signatureKeypair = generateSignatureKeypair(CipherSuite.CURVE25519_AES128)

    return Client(
        id = userId.toByteArray(),
        signatureKeypair = signatureKeypair,
        clientConfig = ClientConfig(
            groupStateStorage = storage,
            useRatchetTreeExtension = true
        )
    )
}

/**
 * Properly cleans up all MLS resources
 */
fun cleanupResources(
    client1: Client?,
    client2: Client?,
    group1: Group?,
    group2: Group?,
    encryptedMessage: Message?,
    receivedMessage: ReceivedMessage?
) {
    // Destroy in reverse order of creation
    receivedMessage?.destroy()
    encryptedMessage?.destroy()
    group2?.destroy()
    group1?.destroy()
    client2?.destroy()
    client1?.destroy()

    println("MLS All resources cleaned up")
}