// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.Parameters

// ALU operation types supported by the processor
object ALUFunctions extends ChiselEnum {
  val zero, add, sub, sll, slt, xor, or, and, srl, sra, sltu, sh1add, sh2add, sh3add,
    mul, mulh, mulhsu, mulhu, div, divu, rem, remu = Value
}

// Arithmetic Logic Unit: performs arithmetic and logical operations
//
// Implements all RV32I ALU operations:
// - Arithmetic: ADD, SUB
// - Logical: AND, OR, XOR
// - Shift: SLL (logical left), SRL (logical right), SRA (arithmetic right)
// - Comparison: SLT (signed), SLTU (unsigned)
// - Special: ZERO (always outputs 0, used for non-ALU instructions)
//
// All operations are combinational, producing results in the same cycle.
class ALU extends Module {
  val io = IO(new Bundle {
    val func = Input(ALUFunctions())

    val op1 = Input(UInt(Parameters.DataWidth))
    val op2 = Input(UInt(Parameters.DataWidth))

    val result = Output(UInt(Parameters.DataWidth))
  })

  // Precompute shared operands/results to keep the case body small
  val mul_ss  = (io.op1.asSInt * io.op2.asSInt)
  val mul_su  = (io.op1.asSInt * Cat(0.U(1.W), io.op2).asSInt)
  val mul_uu  = (io.op1 * io.op2)
  val div_by_zero   = io.op2 === 0.U
  val div_overflow  = (io.op1 === "h80000000".U) && (io.op2 === "hffffffff".U)
  val div_s_quotient = Mux(div_by_zero, (-1).S(32.W), Mux(div_overflow, "h80000000".U.asSInt, (io.op1.asSInt / io.op2.asSInt)))
  val div_u_quotient = Mux(div_by_zero, Fill(32, 1.U(1.W)), (io.op1 / io.op2))
  val rem_s_value    = Mux(div_by_zero, io.op1.asSInt, Mux(div_overflow, 0.S(32.W), (io.op1.asSInt % io.op2.asSInt)))
  val rem_u_value    = Mux(div_by_zero, io.op1, (io.op1 % io.op2))

  io.result := 0.U
  switch(io.func) {
    is(ALUFunctions.add) {
      io.result := io.op1 + io.op2
    }
    is(ALUFunctions.sub) {
      io.result := io.op1 - io.op2
    }
    is(ALUFunctions.sll) {
      io.result := io.op1 << io.op2(4, 0)
    }
    is(ALUFunctions.slt) {
      io.result := io.op1.asSInt < io.op2.asSInt
    }
    is(ALUFunctions.xor) {
      io.result := io.op1 ^ io.op2
    }
    is(ALUFunctions.or) {
      io.result := io.op1 | io.op2
    }
    is(ALUFunctions.and) {
      io.result := io.op1 & io.op2
    }
    is(ALUFunctions.srl) {
      io.result := io.op1 >> io.op2(4, 0)
    }
    is(ALUFunctions.sra) {
      io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt
    }
    is(ALUFunctions.sltu) {
      io.result := io.op1 < io.op2
    }
    is(ALUFunctions.sh1add) {
      io.result := io.op1 + (io.op2 << 1)
    }
    is(ALUFunctions.sh2add) {
      io.result := io.op1 + (io.op2 << 2)
    }
    is(ALUFunctions.sh3add) {
      io.result := io.op1 + (io.op2 << 3)
    }
    is(ALUFunctions.mul) {
      io.result := mul_uu(31, 0)
    }
    is(ALUFunctions.mulh) {
      io.result := mul_ss(63, 32).asUInt
    }
    is(ALUFunctions.mulhsu) {
      io.result := mul_su(63, 32).asUInt
    }
    is(ALUFunctions.mulhu) {
      io.result := mul_uu(63, 32)
    }
    is(ALUFunctions.div) {
      io.result := div_s_quotient.asUInt
    }
    is(ALUFunctions.divu) {
      io.result := div_u_quotient
    }
    is(ALUFunctions.rem) {
      io.result := rem_s_value.asUInt
    }
    is(ALUFunctions.remu) {
      io.result := rem_u_value
    }
  }
}
