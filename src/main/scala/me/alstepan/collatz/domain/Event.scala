package me.alstepan.collatz.domain

final case class Event(machineId: Int, sequenceId: Long, state: Long)