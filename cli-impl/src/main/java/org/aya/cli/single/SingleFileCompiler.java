// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.cli.single;

import org.aya.cli.utils.CliEnums;
import org.aya.cli.utils.CompilerUtil;
import org.aya.producer.AyaParserImpl;
import org.aya.resolve.context.EmptyContext;
import org.aya.resolve.context.ModuleContext;
import org.aya.resolve.module.CachedModuleLoader;
import org.aya.resolve.module.FileModuleLoader;
import org.aya.resolve.module.ModuleCallback;
import org.aya.resolve.module.ModuleListLoader;
import org.aya.util.error.SourceFileLocator;
import org.aya.util.reporter.CollectingReporter;
import org.aya.util.reporter.Reporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

public record SingleFileCompiler(
  @NotNull Reporter reporter,
  @Nullable SourceFileLocator locator
) {
  public <E extends IOException> int compile(
    @NotNull Path sourceFile,
    @NotNull CompilerFlags flags,
    @Nullable ModuleCallback<E> moduleCallback
  ) throws IOException {
    return compile(sourceFile, reporter -> new EmptyContext(reporter, sourceFile).derive("Mian"), flags, moduleCallback);
  }

  public <E extends IOException> int compile(
    @NotNull Path sourceFile,
    @NotNull Function<Reporter, ModuleContext> context,
    @NotNull CompilerFlags flags,
    @Nullable ModuleCallback<E> moduleCallback
  ) throws IOException {
    var reporter = CollectingReporter.delegate(this.reporter);
    var locator = this.locator != null ? this.locator : new SourceFileLocator.Module(flags.modulePaths());
    return CompilerUtil.catching(reporter, flags, () -> {
      var ctx = context.apply(reporter);
      var ayaParser = new AyaParserImpl(reporter);
      var fileManager = new SingleAyaFile.Factory(reporter);
      var ayaFile = fileManager.createAyaFile(locator, sourceFile);
      var program = ayaFile.parseMe(ayaParser);
      ayaFile.pretty(flags, program, reporter, CliEnums.PrettyStage.raw);
      var loader = new CachedModuleLoader<>(new ModuleListLoader(reporter, flags.modulePaths().view().map(path ->
        new FileModuleLoader(locator, path, reporter, ayaParser, fileManager)).toImmutableSeq()));
      // loader.tyckModule(, (moduleResolve, defs) -> {
      //   ayaFile.tyckAdditional(moduleResolve);
      //   ayaFile.pretty(flags, program, reporter, CliEnums.PrettyStage.scoped);
      //   ayaFile.pretty(flags, defs, reporter, CliEnums.PrettyStage.typed);
      //   ayaFile.pretty(flags, program, reporter, CliEnums.PrettyStage.literate);
      //   if (moduleCallback != null) moduleCallback.onModuleTycked(moduleResolve, defs);
      // });
      throw new UnsupportedOperationException("TODO");
    });
  }
}
