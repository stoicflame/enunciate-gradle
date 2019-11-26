/*
 * Copyright 2015 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.webcohesion.enunciate.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import com.webcohesion.enunciate.Enunciate;
import com.webcohesion.enunciate.module.EnunciateModule;

/**
 * Enunciate task.
 * 
 * Provides Gradle DSL for configuration of the Enunciate API.
 * Only Java / Docs output tested at this time.
 * 
 * @author Jesper Skov
 */
public class EnunciateTask extends DefaultTask {
	// Copied from Enunciate Maven plugin
	private static final List<String> VALID_CLASSPATH_TYPES = Arrays.asList("jar", "bundle", "eclipse-plugin", "ejb", "ejb-client");

	private final Logger log;
	private final Property<String> buildDirectoryName;
	private final DirectoryProperty buildDirectory;
	private final Property<String> configurationFileName;
	private final RegularFileProperty configurationFile;
	private final Property<String> classpathConfigName;
	private final JavaPluginConvention javaPluginConvention;
	private final ListProperty<String> extraJavacArgs;
	private final MapProperty<String, File> exports;
	private final PatternFilterable filter = new PatternSet();
	private final SourceSet mainSourceSet;
	private final ConfigurableFileCollection sourcePath;
	private final Property<FileTree> matchingSourceFiles;
	private Configuration enunciateModuleConfiguration;

	public EnunciateTask() {
		ObjectFactory of = getProject().getObjects();
		ProjectLayout layout = getProject().getLayout();

		log = getLogger();

		buildDirectoryName = of.property(String.class)
				.convention("enunciate");
		buildDirectory = of.directoryProperty()
				.convention(layout.getBuildDirectory().dir(buildDirectoryName));
		configurationFileName = of.property(String.class)
				.convention("src/main/enunciate/enunciate.xml");
		configurationFile = of.fileProperty()
				.convention(layout.getProjectDirectory().file(configurationFileName));
		
		classpathConfigName = of.property(String.class)
				.convention("compileClasspath");
		
		javaPluginConvention = getProject().getConvention().findPlugin(JavaPluginConvention.class);

		extraJavacArgs = of.listProperty(String.class);
		exports = of.mapProperty(String.class, File.class);
		
		dependsOn(getProject().getTasks().getByName("classes"));
		
		mainSourceSet = javaPluginConvention.getSourceSets().findByName("main");
		sourcePath = getProject().files();

		matchingSourceFiles = of.property(FileTree.class)
			.convention(getProject().provider(() -> mainSourceSet.getAllJava().getAsFileTree().matching(filter)));
	}

	public void setEnunciateModuleConfig(Configuration configuration) {
		enunciateModuleConfiguration = configuration;
	}

	public void setClasspathConfigName(Provider<String> configNameProvider) {
		classpathConfigName.set(configNameProvider);
	}
	
	public void setClasspathConfigName(String configName) {
		classpathConfigName.set(configName);
	}
	
	@Input
	public Property<String> getClasspathConfigName() {
		return classpathConfigName;
	}

	@InputFiles
	public Property<FileTree> getMatchingSourceFiles() {
		return matchingSourceFiles;
	}
	
	public void exclude(String pattern) {
		filter.exclude(pattern);
	}
	
	public void include(String pattern) {
		filter.include(pattern);
	}
	
	@OutputDirectories
	public MapProperty<String, File> getExports() {
		return exports;
	}
	
	public void export(String id, File destination) {
		exports.put(id, destination);
	}

	public void sourcepath(Object... sourcePaths) {
		sourcePath.from(sourcePaths);
	}

	@OutputDirectory
	public DirectoryProperty getBuildDir() {
		return buildDirectory;
	}

	@InputFile
	public RegularFileProperty getConfigurationFile() {
		return configurationFile;
	}

	@Input
	public ListProperty<String> getExtraJavacArgs() {
		return extraJavacArgs;
	}

	public void setExtraJavacArgs(List<String> extraJavacArgs) {
		this.extraJavacArgs.set(extraJavacArgs);
	}

	@Input
	public Property<String> getBuildDirectoryName() {
		return buildDirectoryName;
	}

	public void setBuildDirName(String buildDirName) {
		buildDirectoryName.set(buildDirName);
	}

	@Input
	public Property<String> getConfigurationFileName() {
		return configurationFileName;
	}

	public void setConfigFileName(String configFileName) {
		configurationFileName.set(configFileName);
	}

	@TaskAction
	public void run() {
		File configFile = configurationFile.get().getAsFile();
		if (!configFile.exists()) {
			log.info("Enunciate task did nothing - did not find cofiguration file {}", configFile);
		}
		
		List<URL> moduleUrls = enunciateModuleConfiguration.getFiles().stream()
			.map(this::fileToUrl)
			.collect(Collectors.toList());
		URL[] moduleUrlsArray = moduleUrls.toArray(new URL[moduleUrls.size()]);
		
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try (URLClassLoader moduleClassloader = new URLClassLoader(moduleUrlsArray, contextClassLoader)) {
			Thread.currentThread().setContextClassLoader(moduleClassloader);

			printFoundEnunciateModules();
			configureAndInvokeEnunciate();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to invoke Enunciate", e);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	private void printFoundEnunciateModules() {
		ServiceLoader<EnunciateModule> moduleLoader = ServiceLoader.load(EnunciateModule.class);
		log.info("Modules:");
		moduleLoader.forEach(em -> log.info(" {}", em));
	}

	private void configureAndInvokeEnunciate() {
		Set<File> inputFiles = matchingSourceFiles.get().getFiles();
		
		Enunciate enunciate = new Enunciate();
		enunciate.setLogger(new EnunciateLoggerBridge(log));
		enunciate.setBuildDir(buildDirectory.get().getAsFile());
		enunciate.setSourceFiles(inputFiles);
		enunciate.setClasspath(getClasspathJars());
		enunciate.getCompilerArgs().addAll(buildCompilerArgs(javaPluginConvention));
		enunciate.getCompilerArgs().addAll(extraJavacArgs.get());
		
		ArrayList<File> sourcePathArg = new ArrayList<>(sourcePath.getFiles());
		log.info("Adding sourcepath {}", sourcePathArg);
		enunciate.setSourcepath(sourcePathArg);
		
		File configFile = configurationFile.get().getAsFile();
		log.info("Using config {}", configFile);
		enunciate.loadConfiguration(configFile);
		enunciate.getConfiguration().setBase(configFile);
		
		enunciate.loadDiscoveredModules();

		for (Map.Entry<String, File> export : exports.get().entrySet()) {
			log.info("Adding export {} : {}", export.getKey(), export.getValue());
			enunciate.addExport(export.getKey(), export.getValue());
		}
		
		enunciate.run();
	}

	private URL fileToUrl(File f) {
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Bad file->URL conversion for " + f, e);
		}
	}

	// Filters out files that are invalid in classpath context.
	// An example is .pom files which may appear when BOMs are used.
	private List<File> getClasspathJars() {
		return getProject().getConfigurations().getByName(classpathConfigName.get())
				.getFiles()
				.stream()
				.filter(this::isValidClasspathElement)
				.collect(Collectors.toList());
	}
	
	private boolean isValidClasspathElement(File f) {
		String type = f.getName().replaceFirst(".*[.]", "").toLowerCase();
		boolean include = f.isDirectory() || VALID_CLASSPATH_TYPES.contains(type);
		log.debug("Include {} , type '{}' : {}", f.getName(), type, include);
		return include;
	}
	
	private List<String> buildCompilerArgs(JavaPluginConvention javaPluginConvention) {
		TaskCollection<JavaCompile> javaCompilers = getProject().getTasks().withType(JavaCompile.class);
		CompileOptions firstCompilerOptions = javaCompilers.isEmpty() ? null : javaCompilers.iterator().next().getOptions();
		
		List<String> args = new ArrayList<>(Arrays.asList("-source", javaPluginConvention.getSourceCompatibility().toString(),
							 							  "-target", javaPluginConvention.getTargetCompatibility().toString(),
							 							  "-encoding", getDefaultOrCompilerEncoding(firstCompilerOptions)));

		if (firstCompilerOptions != null) {
			FileCollection bootClasspath = firstCompilerOptions.getBootstrapClasspath();
			if (bootClasspath != null) {
				args.add("-bootclasspath");
				args.add(bootClasspath.getAsPath());
			}
		}
		
		return args;
	}

	private String getDefaultOrCompilerEncoding(CompileOptions compileOptions) {
		String compilerEncoding = compileOptions != null ? compileOptions.getEncoding() : null;
		return compilerEncoding == null ? Charset.defaultCharset().name() : compilerEncoding;
	}
}
