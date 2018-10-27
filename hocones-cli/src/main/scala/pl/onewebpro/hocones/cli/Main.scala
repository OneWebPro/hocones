package pl.onewebpro.hocones.cli

import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import pl.onewebpro.hocon.utils.parser.{HoconParser, HoconResult}
import pl.onewebpro.hocones.cli.Properties.ProgramMode
import pl.onewebpro.hocones.statistics.StatisticsMeta
import cats.effect.Console.io._
import pl.onewebpro.hocon.utils.env.EnvironmentFileGenerator

object Main extends IOApp {

  def runApp(properties: Properties.CliProperties, parsedFile: HoconResult): IO[Unit] =
    properties.mode match {
      case ProgramMode.EnvFile => for {
        _ <- putStrLn("Generating environment file")
        _ <- EnvironmentFileGenerator(properties.envConfiguration, parsedFile).toIO
        _ <- putStrLn(s"File generated ${properties.envConfiguration.outputPath}")
      } yield ()
      case ProgramMode.Load => for {
        _ <- putStrLn("Generating statistics")
        statistics <- StatisticsMeta.fromParsedHocon(parsedFile).toIO
        _ <- putStrLn(
          s"""
             |Number of paths: ${statistics.numOfPaths}
             |Number of environment values: ${statistics.numOfEnvironmentValues}
             |Number of not resolved references: ${statistics.numOfNotResolvedRef}
             |Number of resolved references: ${statistics.numOfResolvedRef}
           """.stripMargin)
      } yield ()
    }

  override def run(args: List[String]): IO[ExitCode] =
    Properties.parseArgs(args).toIO.flatMap {
      case Right(properties) => for {
        _ <- putStrLn("Loading configurations")
        parsedFile <- HoconParser(ConfigFactory.parseFile(properties.input))
        _ <- putStrLn("Configuration parsed without errors")
        _ <- runApp(properties, parsedFile)
        _ <- putStrLn("Done. Bye bye!")
      } yield ExitCode.Success
      case Left(_) => IO.pure(ExitCode.Error)
    }

}