import net.darkhax.curseforgegradle.TaskPublishCurseForge
import net.darkhax.curseforgegradle.Constants as CFG_Constants

plugins {
	id("net.darkhax.curseforgegradle") version("1.1.18")
	id("com.modrinth.minotaur") version("2.+")
}

// gradle.properties
val curseHomepageLink: String by extra
val curseProjectId: String by extra
val modrinthProjectId: String by extra
val fabricVersion: String by extra
val fabricLoaderVersion: String by extra
val clothVersion: String by extra
val modMenuVersion: String by extra
val minecraftVersion: String by extra
val modId: String by extra
val modFileName: String by extra
val modJavaVersion: String by extra

val baseArchivesName = "${modFileName}-Fabric-${minecraftVersion}"
base {
	archivesName.set(baseArchivesName)
}

architectury {
	// Create the IDE launch configurations for this subproject.
	platformSetupLoomIde()
	// Set up Architectury for Fabric.
	fabric()
}

loom {
	accessWidenerPath.set(project(":Common").file("src/main/resources/betteradvancements.accesswidener"))
}

dependencies {
	modImplementation("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
	modApi("me.shedaniel.cloth:cloth-config-fabric:${clothVersion}") {
		exclude("net.fabricmc.fabric-api")
	}
	modImplementation("com.terraformersmc:modmenu:${modMenuVersion}")

	implementation(project(":Common", configuration = "namedElements")) { isTransitive = false }
	shadowImplementation(project(":Common", configuration = "transformProductionFabric")) { isTransitive = false }

	implementation(project(":FabricApi", configuration = "namedElements"))
	shadowImplementation(project(":CommonApi")) { isTransitive = false }
	shadowImplementation(project(":FabricApi")) { isTransitive = false }
}

val apiJar = tasks.register<Jar>("apiJar") {
	from(project(":CommonApi").sourceSets.main.get().output)
	from(project(":FabricApi").sourceSets.main.get().output)

	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveClassifier.set("api")
}

artifacts {
	archives(apiJar.get())
	archives(tasks.jar.get())
	archives(tasks.remapJar.get())
	archives(tasks.remapSourcesJar.get())
}

tasks.withType<Jar> {
	destinationDirectory.set(file(rootProject.rootDir.path + "/output"))
}

tasks.withType<net.fabricmc.loom.task.RemapJarTask> {
	destinationDirectory.set(file(rootProject.rootDir.path + "/output"))
}

tasks.withType<ProcessResources> {
	from(project(":Common").sourceSets.main.get().resources)
}

tasks.register<TaskPublishCurseForge>("publishCurseForge") {

	apiToken = System.getenv("CURSE_KEY") ?: "0"

	val mainFile = upload(curseProjectId, tasks.remapJar.get())
	mainFile.changelogType = CFG_Constants.CHANGELOG_MARKDOWN
	mainFile.changelog = System.getenv("CHANGELOG") ?: ""
	mainFile.releaseType = CFG_Constants.RELEASE_TYPE_ALPHA
	mainFile.addModLoader("Fabric")
	mainFile.addJavaVersion("Java $modJavaVersion")
	mainFile.addGameVersion(minecraftVersion)
	mainFile.withAdditionalFile(apiJar.get())
	mainFile.withAdditionalFile(tasks.remapSourcesJar.get())
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN") ?: "0")
	projectId.set(modrinthProjectId)
	versionNumber.set("${project.version}")
	versionName.set("${project.version} for Fabric $minecraftVersion")
	versionType.set("alpha")
	uploadFile.set(tasks.remapJar.get())
	gameVersions.add(minecraftVersion)
	// additionalFiles.addAll(arrayOf(apiJar.get(), tasks.remapSourcesJar.get())) // TODO: Figure out how to upload these
}
tasks.modrinth.get().dependsOn(tasks.remapJar)
