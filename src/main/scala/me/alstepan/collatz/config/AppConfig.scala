package me.alstepan.collatz.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class AppConfig(collatzConfig: CollatzConfig, emberConfig: EmberConfig) derives ConfigReader