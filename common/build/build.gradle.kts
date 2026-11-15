plugins {
    `kotlin-dsl`
}

repositories { mavenCentral() }

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

gradlePlugin {
    plugins {
        create("hellenicclient") {
            id = "hellenicclient"
            implementationClass = "gr.hellenic.client.HellenicClientGradlePlugin"
        }
    }
}