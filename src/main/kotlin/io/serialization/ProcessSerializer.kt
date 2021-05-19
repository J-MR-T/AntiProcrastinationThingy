package io.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import processes.Process
import processes.implementations.ProcessIdentifier


object ProcessSerializer : KSerializer<Process> {
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Process {
        val (command, stringRepresentation) = decoder.decodeStructure(descriptor) {
            var command = ""
            var stringRepresentation = ""
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> command = decodeStringElement(descriptor, index)
                    1 -> stringRepresentation = decodeStringElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            command to stringRepresentation
        }
        return ProcessIdentifier(command, stringRepresentation)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Process") {
        element<String>("command")
        element<String>("stringRepresentation")
    }


    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Process) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.command);
        encodeStringElement(descriptor, 1, value.stringRepresentation);
    }

}