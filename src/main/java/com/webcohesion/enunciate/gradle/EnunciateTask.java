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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import com.webcohesion.enunciate.Enunciate;

/**
 * Enunciate task.
 * 
 * Provides Gradle DSL for configuration of the Enunciate API.
 * Only Java / Docs output tested at this time.
 * 
 * @author Jesper Skov
 */
public class EnunciateTask extends DefaultTask {
	private String buildDirName = "enunciate";
	private String configFileName = "src/main/enunciate/enunciate.xml";
	private Property<String> classpathConfigName;
	private Logger log;
	private PatternFilterable filter = new PatternSet();
	private Map<String, File> exports = new HashMap<>();
	private SourceSet mainSourceSet;
	private JavaPluginConvention javaPluginConvention;
	private List<String> extraJavacArgs = new ArrayList<>();
	private ConfigurableFileCollection sourcePath;
	
	public EnunciateTask() {
		log = getLogger();

		classpathConfigName = getProject().getObjects().property(String.class);
		classpathConfigName.set("compileClasspath");
		
		javaPluginConvention = getProject().getConvention().findPlugin(JavaPluginConvention.class);

		dependsOn(getProject().getTasks().getByName("classes"));
		
		mainSourceSet = javaPluginConvention.getSourceSets().findByName("main");
		sourcePath = getProject().files();
		
		getInputs().files(lazyGetMatchingSourceFiles());
		getInputs().file(lazyGetConfigFile());
		getOutputs().dir(lazyGetBuildDir());
	}

	public void setClasspathConfigName(Provider<String> configNameProvider) {
		classpathConfigName.set(configNameProvider);
	}
	
	public void setClasspathConfigName(String configName) {
		classpathConfigName.set(configName);
	}
	
	public String getClasspathConfigName() {
		return classpathConfigName.get();
	}
	
	public void exclude(String pattern) {
		filter.exclude(pattern);
	}
	
	public void include(String pattern) {
		filter.include(pattern);
	}
	
	public void export(String id, File destination) {
		getOutputs().dir(destination);
		exports.put(id, destination);
	}

	public void sourcepath(Object... sourcePaths) {
		sourcePath.from(sourcePaths);
	}
	
	public File getBuildDir() {
		return new File(getProject().getBuildDir(), buildDirName);
	}

	public File getConfigFile() {
		return getProject().file(configFileName);
	}

	public List<String> getExtraJavacArgs() {
		return extraJavacArgs;
	}

	public void setExtraJavacArgs(List<String> extraJavacArgs) {
		this.extraJavacArgs = extraJavacArgs;
	}
	
	public String getBuildDirName() {
		return buildDirName;
	}

	public void setBuildDirName(String buildDirName) {
		this.buildDirName = buildDirName;
	}

	public String getConfigFileName() {
		return configFileName;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}

	@TaskAction
	public void run() {
		if (!getConfigFile().exists()) {
			log.info("Enunciate task did nothing - did not find cofiguration file {}", getConfigFile());
		}
		
		Set<File> inputFiles = getMatchingSourceFiles().getFiles();
		
		Enunciate enunciate = new Enunciate();
		enunciate.setLogger(new EnunciateLoggerBridge(log));
		enunciate.setBuildDir(getBuildDir());
		enunciate.setSourceFiles(inputFiles);
		enunciate.setClasspath(getClasspathJars());
		enunciate.getCompilerArgs().addAll(buildCompilerArgs(javaPluginConvention));
		enunciate.getCompilerArgs().addAll(extraJavacArgs);
		
		ArrayList<File> sourcePathArg = new ArrayList<>(sourcePath.getFiles());
		log.info("Adding sourcepath {}", sourcePathArg);
		enunciate.setSourcepath(sourcePathArg);
		
		log.info("Using config {}", getConfigFile());
		enunciate.loadConfiguration(getConfigFile());
		enunciate.getConfiguration().setBase(getConfigFile().getParentFile());
		
		enunciate.loadDiscoveredModules();

		for (Map.Entry<String, File> export : exports.entrySet()) {
			log.info("Adding export {} : {}", export.getKey(), export.getValue());
			enunciate.addExport(export.getKey(), export.getValue());
		}
		
		enunciate.run();
	}

	// Filters out .pom files which may appear when BOMs are used
	private List<File> getClasspathJars() {
		return getProject().getConfigurations().getByName(classpathConfigName.get())
				.getFiles()
				.stream()
				.filter(f -> !f.getName().endsWith(".pom"))
				.collect(Collectors.toList());
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
	
	
	private FileTree getMatchingSourceFiles() {
		return mainSourceSet.getAllJava().getAsFileTree().matching(filter);
	}
	
	private Callable<FileTree> lazyGetMatchingSourceFiles() {
		return new Callable<FileTree>() {
			@Override
			public FileTree call() {
				return getMatchingSourceFiles();
			}
		};
	}

	private Callable<File> lazyGetConfigFile() {
		return new Callable<File>() {
			@Override
			public File call() {
				return getConfigFile();
			}
		};
	}

	private Callable<File> lazyGetBuildDir() {
		return new Callable<File>() {
			@Override
			public File call() {
				return getBuildDir();
			}
		};
	}
}
