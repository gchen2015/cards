package rmg.apps.cards.base.model

import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.quality.Strictness
import rmg.apps.cards.base.SignifiedCriteria
import rmg.apps.cards.base.SignifiedRepository
import rmg.apps.cards.base.SignifierCriteria
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class MultipleChoiceQuestionGeneratorUnitTest {

    companion object TestData {
        const val NUM_ANSWERS = 5
    }

    @Rule
    @JvmField
    var expectedException = ExpectedException.none()

    @Rule
    @JvmField
    var mockito = MockitoJUnit.rule()
        .strictness(Strictness.STRICT_STUBS)

    lateinit var generator: MultipleChoiceQuestion.Generator<Int, Unit>

    @Mock
    lateinit var repository: SignifiedRepository<Int, Unit>

    val answerCriteria: SignifierCriteria = SignifierCriteria.WrittenWordCriteria(locale = Signifier.Locale("eng"))

    @Before
    fun setUp() {
        generator = MultipleChoiceQuestion.Generator(
            repository = repository,
            numAnswers = NUM_ANSWERS,
            answerCriteria = answerCriteria)
    }

    @Test
    fun testGenerateQuestion_SignifiedMustHaveSignifiers() {
        expectedException.expect(IllegalArgumentException::class.java)

        val questionSignified = Signified(type = Signified.Type.NOUN,
            signifiers = emptyList())

        generator.generateQuestion(questionSignified)
    }

    @Test
    fun testGenerateQuestion_Success() {
        val questionSignifier = WrittenWord(lang = "eng", word = "Test")
        val questionSignified = Signified(type = Signified.Type.NOUN, signifiers = listOf(
            questionSignifier,
            WrittenWord(lang = "zho", script = "Hans", word = "考试")))
        val answerSignifieds = listOf(
            Signified(type = Signified.Type.NOUN, signifiers = listOf(WrittenWord(lang = "zho", script = "Hans", word = "北京"))),
            Signified(type = Signified.Type.NOUN, signifiers = listOf(WrittenWord(lang = "eng", word = "Something Not a Test")))
        )
        val containsAnswerCriteria = SignifiedCriteria.ContainsSignifier(answerCriteria)

        given {
            repository.find(maxResults = eq(NUM_ANSWERS - 1), order = anyOrNull(), user = anyOrNull(), criteria = argWhere {
                it.contains(containsAnswerCriteria)
            })
        } willReturn {
            answerSignifieds.filter(containsAnswerCriteria::match)
                .mapIndexed { index, signified -> SignifiedRepository.StoredSignified(index, signified) }
        }

        val question = generator.generateQuestion(questionSignified)

        if (question !is MultipleChoiceQuestion) {
            fail("Wrong question type generated by MultipleChoiceQuestion.Generator: ${question::class}")
        }

        val expectedAnswerSignifiers = listOf(questionSignifier) + answerSignifieds
            .flatMap { signified -> signified.signifiers }
            .filter { signifier ->
                if (signifier is WrittenWord) {
                    signifier.locale.lang == "eng"
                } else {
                    false
                }
            }

        assertTrue {
            question.answerSignifiers.containsAll(expectedAnswerSignifiers + questionSignifier)
        }
        assertEquals(expectedAnswerSignifiers.size, question.answerSignifiers.size)
    }
}

