package com.example.shulecard.utils

/**
 * Shared list of security questions used at account creation (admin, student, parent, vendor)
 * to power self-service password recovery, since this app has no backend to email/SMS a reset
 * link through.
 */
object SecurityQuestions {

    val QUESTIONS = listOf(
        "What is your mother's maiden name?",
        "What was the name of your first school?",
        "What is your favorite teacher's name?",
        "What city were you born in?",
        "What was the name of your first pet?"
    )

    /** Case/whitespace-insensitive so a slightly different typing of the same answer still matches. */
    fun normalize(answer: String): String = answer.trim().lowercase()
}
