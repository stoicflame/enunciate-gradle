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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskDependency;
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
	public String buildDirName = "enunciate";
	public String configFileName = "src/main/enunciate/enunciate.xml";
	
	private Logger log;
	private PatternFilterable filter = new PatternSet();
	private Map<String, File> exports = new HashMap<>();
	private SourceSet mainSourceSet;
	private JavaPluginConvention javaPluginConvention;
	private List<String> extraJavacArgs = new ArrayList<>();
	
	public EnunciateTask() {
		log = getLogger();

		javaPluginConvention = getProject().getConvention().findPlugin(JavaPluginConvention.class);
		if (javaPluginConvention != null) {
			setup();
		} else {
			log.debug("Enunciate disabled as did not find java plugin");
		}
	}
	
	private void setup() {
		TaskDependency buildDependencies = getProject().getConfigurations().getByName("compile").getBuildDependencies();
		dependsOn(buildDependencies);
		
		mainSourceSet = javaPluginConvention.getSourceSets().findByName("main");
		
		getInputs().file(lazyGetMatchingSourceFiles());
		getInputs().file(lazyGetConfigFile());
		getOutputs().dir(lazyGetBuildDir());
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

	@TaskAction
	public void run() {
		Set<File> inputFiles = getMatchingSourceFiles().getFiles();
		
		Enunciate enunciate = new Enunciate();
		enunciate.setLogger(new EnunciateLoggerBridge(log));
		enunciate.setBuildDir(getBuildDir());
		enunciate.setSourceFiles(inputFiles);
		enunciate.setClasspath(new ArrayList<>(mainSourceSet.getCompileClasspath().getFiles()));
		enunciate.getCompilerArgs().addAll(buildCompilerArgs(javaPluginConvention));
		enunciate.getCompilerArgs().addAll(extraJavacArgs);
		
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
	
	private List<String> buildCompilerArgs(JavaPluginConvention javaPluginConvention) {
		TaskCollection<JavaCompile> javaCompilers = getProject().getTasks().withType(JavaCompile.class);
		CompileOptions firstCompilerOptions = javaCompilers.isEmpty() ? null : javaCompilers.iterator().next().getOptions();
		
		List<String> args = new ArrayList<>(Arrays.asList("-source", javaPluginConvention.getSourceCompatibility().toString(),
							 							  "-target", javaPluginConvention.getTargetCompatibility().toString(),
							 							  "-encoding", getDefaultOrCompilerEncoding(firstCompilerOptions)));

		String bootClasspath = firstCompilerOptions.getBootClasspath();
		if (bootClasspath != null) {
			args.add("-bootclasspath");
			args.add(bootClasspath);
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
			public FileTree call() {
				return getMatchingSourceFiles();
			}
		};
	}

	private Callable<File> lazyGetConfigFile() {
		return new Callable<File>() { 
			public File call() {
				return getConfigFile();
			}
		};
	}

	private Callable<File> lazyGetBuildDir() {
		return new Callable<File>() { 
			public File call() {
				return getBuildDir();
			}
		};
	}
}
