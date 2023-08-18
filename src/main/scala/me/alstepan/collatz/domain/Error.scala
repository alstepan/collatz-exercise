package me.alstepan.collatz.domain

sealed trait Error

case class MachineAlreadyExists(id: Int) extends Error

case class MachineNotFound(id: Int) extends Error
