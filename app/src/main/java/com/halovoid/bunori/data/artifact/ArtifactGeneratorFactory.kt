package com.halovoid.bunori.data.artifact

class ArtifactGeneratorFactory(
    private val generator: List<ArtifactGenerator>
) {
    fun getGenerator(format: String?): ArtifactGenerator {
        return generator.find { it.format.equals(format, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported format: $format")
    }
}