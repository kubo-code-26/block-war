package com.blockwar.tutorial

import org.junit.Assert.*
import org.junit.Test

class TutorialSessionTest {
    @Test fun movementLessonRequiresActuallyFillingTheGap() {
        assertFalse(TutorialSession().drop().completed)
        var session=TutorialSession()
        repeat(3) { session=session.move(1) }
        session=session.drop()
        assertTrue(session.completed)
        assertEquals(1,session.clearedLines)
        assertEquals(5,session.attack)
        assertEquals(1,session.next().lesson)
    }
    @Test fun rotationLessonUsesRealThreeLineClear() {
        var session=TutorialSession.start(1)
        repeat(2) {session=session.move(1)}
        assertFalse(session.drop().completed)
        session=session.rotate().drop()
        assertTrue(session.completed)
        assertEquals(3,session.clearedLines)
        assertEquals(12,session.attack)
    }
    @Test fun finalAttackAndRepeatedDropCannotDuplicateProgress() {
        var session=TutorialSession.start(2)
        repeat(3) {session=session.move(1)}
        session=session.drop()
        assertTrue(session.completed)
        assertEquals(5,session.attack)
        assertEquals(session,session.drop())
        assertEquals(session,session.next())
        assertEquals(session,session.move(-1))
    }
    @Test fun wallsAndRetryKeepPracticeRecoverable() {
        var session=TutorialSession()
        repeat(30) {session=session.move(-1)}
        assertEquals(0,session.x)
        repeat(30) {session=session.move(1)}
        assertEquals(10,session.x)
        val retry=session.drop()
        assertTrue(retry.retry)
        assertFalse(retry.completed)
        assertEquals(0,retry.next().lesson)
        assertEquals(TutorialSession().board,retry.board)
    }
}
