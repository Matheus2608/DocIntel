package dev.matheus.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.ContainsString;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotIn;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;

@QuarkusTest
class RetrievalInfoServiceTest {

//    BEGIN;
//    DELETE FROM public.embeddings
//    WHERE metadata->>'file_name' = 'Guia do Atleta 2025.pdf'
//    OR metadata->>'FILE_NAME' = 'Guia do Atleta 2025.pdf';
//    COMMIT;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    @Named("embeddingModel")
    EmbeddingModel embeddingModel;


    private static final String PARAGRAPH_KEY = "PARAGRAPH";
    private static final String FILE_NAME_KEY = "FILE_NAME";

    private static final double MINIMUM_SIMILARITY_SCORE = 0.75f;
    private static final double MINIMUM_SCORE = 0.65f;

    @Test
    void getVector() {
        String question = "quais os tempos de corte e a distância dos pontos de controle para a distancia de 64km?";
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        System.out.println(questionEmbedding.dimension());
        System.out.println(questionEmbedding.vectorAsList());
    }

    @Test
    void testRetrieval() {
        String question = "quais os tempos de corte e a distância dos pontos de controle para a distancia de 66km?";
        String filename = "Guia do Atleta 2025.pdf";
        Embedding questionEmbedding = Embedding.from(getVectorEmbedding());

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .maxResults(20) // Get more results to compensate for filtering
                        .minScore(MINIMUM_SIMILARITY_SCORE)
                        .queryEmbedding(questionEmbedding)
                        .filter(new IsEqualTo(FILE_NAME_KEY, filename))
                        .build());

        yellowPrint(String.format("Direct search returned %d results before filtering", result.matches().size()));
        ensureAllEmbeddingsHaveParagraphMetadata(result.matches());
    }

    private void ensureAllEmbeddingsHaveParagraphMetadata(List<EmbeddingMatch<TextSegment>> matches) {
        AtomicInteger i = new AtomicInteger();

        // Filter results to only include segments with PARAGRAPH_KEY
        matches = matches.stream()
                .filter(match -> match.embedded().metadata().containsKey(PARAGRAPH_KEY))
                .toList();

        for (EmbeddingMatch<TextSegment> match : matches) {
            Metadata metadata = match.embedded().metadata();
            String embedQuestion = match.embedded().text();
            String chunck = match.embedded().metadata().getString(PARAGRAPH_KEY);
            Double score = match.score();
            String printText = String.format("Score: %.4f\n Question: %s\n Text: %s", score, embedQuestion, chunck);
            if (i.get() % 2 == 0) {
                yellowPrint(printText);
            } else {
                greenPrint(printText);
            }
            i.getAndIncrement();

            if (metadata == null)
                continue;

            if (!metadata.containsKey(PARAGRAPH_KEY)) {
                greenPrint("--------------- MISSING PARAGRAPH ---------------");
                yellowPrint("Question: " + match.embedded().text());
                throw new IllegalStateException("Embedding is missing required PARAGRAPH metadata. Question related to embedding: " + match.embedded().text() + "and embeddingId = " + match.embeddingId());
            }
        }
    }

    private List<Float> getVectorEmbedding() {
        String arrayEmString = "[0.03199445, 0.008514653, 0.093299955, -0.047564104, -0.06127601, 0.016439546, 0.011190686, 0.0037855187, -0.02429809, 0.028072553, 0.021614686, -0.016572243, -0.0372728, -0.019181928, 0.050866757, -0.021423014, -0.0045190323, 0.06434276, 0.01986015, 0.062219627, -0.036388163, -0.014758731, -0.014972519, -0.035120178, -0.015761323, -0.02998927, -0.025728257, -0.037331775, 0.020980693, 0.036181744, -0.01579081, -0.038953613, -0.02496157, -0.110344, 0.061099082, 0.02991555, -0.08209452, 0.021496734, -0.03338039, 0.016542753, -0.029163606, 0.020125542, -0.059860587, -0.049126964, 0.0051419656, -0.033262435, -0.025153242, -0.0033726871, -5.0820684E-4, -0.0067638042, 0.03349834, 0.05823875, -0.03287909, 0.035444546, 0.024622459, 0.05900544, 0.075902045, 0.016247874, 0.06475559, -0.013380169, 0.046414074, -0.016129922, -0.0055805994, -0.0062330207, -0.0056358892, 0.0696506, 0.06575818, 0.011146454, -0.023782052, -0.010387139, -0.07613795, 0.061924748, 0.03812795, -0.083627895, 0.049126964, -0.021305062, -0.028426407, -0.037626654, 0.025964161, 0.010003796, 0.01256925, -0.03184701, -0.002355352, 0.007961753, -0.016100435, -0.05605664, -0.101261705, 0.04859618, 0.050424438, -0.025669282, -0.04499865, 0.0527245, 0.012635597, 0.04207934, -0.021201855, 6.381382E-4, 0.022057004, -0.019240903, -0.0063657165, -0.07006343, -0.0062883105, -0.0021655231, 0.025050035, 0.012812525, 0.061924748, 0.031788036, 0.010040656, 0.07967651, 0.049598772, -0.005750155, 0.119721174, 0.00939192, -0.03730229, 0.025020547, 0.12679829, -0.011360242, 0.015879275, -0.060627274, 0.038688224, 0.0073019597, -0.03680099, -0.061629865, -0.008558884, -0.059772126, 0.007954381, -0.028750775, -0.019403087, -0.06339914, 0.02381154, -0.040398527, -0.0044121384, -0.096720554, 0.061806794, -0.058002844, 0.028529616, 0.036535602, 0.024902595, 0.015319003, -0.0025507098, -1.8280241E-4, -0.03199445, 0.02880975, 0.005805445, 4.5291687E-4, 0.019683223, 0.027984088, -0.08645874, 0.023030108, 0.036358673, -0.010158608, -0.02481413, -0.009362432, -0.0027036788, 0.015075727, -0.023457684, -0.010320791, -0.06339914, 0.0029672275, -0.034884274, 0.060332395, 0.042108826, 0.0065131565, 0.02059735, 0.05531944, -0.010394511, -0.031463668, 0.034913763, 0.07254042, -0.09188453, -0.07896879, -0.03724331, -0.042639613, 0.01782548, 0.021231342, -0.035120178, 0.059919566, 0.0076152696, -0.003652823, 0.008381957, 0.035886865, -0.048065398, -0.06729156, 0.038186926, 0.06770439, -0.011374986, 0.05080778, 0.027895624, 0.0605683, 0.009369804, 0.025654538, -0.012031093, -0.028573848, 0.036742017, 0.031109814, -0.007983869, -0.027792417, 0.008595745, -0.088286996, -0.03361629, -0.005772271, 0.014611292, 0.0043457905, 0.009767892, -0.058209263, -0.030520054, -0.040958796, 0.029694391, 0.005742783, 0.014515456, 0.07183271, 0.030490566, -0.017913945, 0.06858903, 0.01608569, 0.007865917, -0.012576621, -0.061511915, 0.057000253, 0.0010855261, -0.00691493, -0.033085506, 0.0127240615, 0.0011896555, 0.05865158, -0.013630817, 0.007158206, 0.04207934, 0.05287194, -0.031935476, 0.019417832, -0.021821102, -0.0068338383, -0.058061823, -0.04965775, 0.031434182, 0.002406956, 3.6721744E-4, -0.009119156, 0.016498521, -0.0434063, -0.03798051, -0.024681434, 0.009266596, 0.01797292, -0.03158162, 0.020243494, -0.016837634, 0.009082296, -0.011854166, -0.03364578, 0.062396556, -0.052636035, -0.009863728, -0.033291925, -0.0054110433, 0.04004467, -0.03553301, -0.007335134, 0.014095252, -0.05941827, -0.033262435, -5.4967427E-4, -0.058209263, -0.020361446, -0.019609503, 0.04482172, 0.062927336, 0.03453042, -0.038953613, -0.015613883, -0.013306448, 0.029782854, 0.019462064, -0.05210525, -0.07265837, 0.0020014963, 0.020715302, 0.0032086603, 0.01638057, -0.03364578, 0.06345812, 0.023575636, -0.012539761, -0.055850223, 0.025654538, 0.00942878, 0.018680632, 0.058504142, 0.040811356, -0.09212043, 0.027880881, 0.045558922, -0.06275041, -0.0022115982, 0.034147076, -0.04992314, -0.050896246, -0.02111339, -0.028043063, -0.025448123, 0.039690815, -0.0119573735, 0.05797336, -0.06982753, -0.010844203, 0.05136805, 0.010482975, 0.034294516, -0.016277362, -0.06593511, 0.038540784, 0.067999266, -0.009414036, -0.02709945, 6.929674E-4, 0.010438743, 0.065876134, 0.0036307068, 0.014117368, -0.062927336, 0.110344, -0.0566464, -0.02447502, 0.04582431, -0.041106235, -0.00471439, -0.025743002, -0.036506113, 0.08297916, -0.015142075, -0.036299698, 0.011411847, -0.017530601, 0.034736834, 0.0063067405, -0.017442137, -0.018253056, -0.020051822, 0.01674917, 0.013815116, -0.040545966, -0.0064578666, -0.026155833, 0.028632823, 0.00499084, 0.019948615, -5.1978313E-5, -0.03709587, 0.057413086, 0.034088098, 0.009679427, -0.0605683, -0.010180723, 0.011257035, 0.05505405, -0.028824495, 0.03305602, 0.046974342, -0.033940658, -0.013299077, 0.0023811539, -0.0015665487, 0.04384862, -0.06658385, -0.044792235, -0.030372614, -0.015201051, -0.01775176, -0.073248126, -0.027718697, 0.037538193, 0.020730047, -0.014817707, -0.0028529617, -0.0088390205, -0.04502814, 0.04384862, -0.0010919765, 0.050984707, 0.012930477, 0.0063399146, -0.017663296, 0.014294296, 0.0052230577, 0.01124229, 0.024651946, 0.052753985, 0.0038002627, 0.029782854, -0.014795591, -0.034235537, 0.08203555, 0.09229736, 0.024843618, -0.015363235, -0.022322398, 0.116595455, 0.012466041, -0.023413451, 0.0013711908, -0.053048868, -0.078261085, -0.051456515, 0.06251451, 0.042315245, 0.0062809386, -0.033144485, -0.019727455, -0.039277982, 0.028588591, 0.0038629246, -0.06687873, 0.040310062, -0.024357067, 0.012517645, 0.013623444, -0.027895624, 0.049333382, -0.047858983, -0.05010007, -0.009790008, 0.06127601, -0.08952549, 0.0193736, -0.08368687, 0.044910185, 0.0029285245, 0.033734243, -0.024489762, -0.038953613, 0.015731834, 0.014714499, 0.07778928, 0.07230452, -0.059860587, 0.049598772, -0.02677508, 0.017781248, -0.036211234, 0.020169774, 0.00906018, -0.027497536, 0.0061703585, 0.016424801, 0.015967738, -0.0015462757, 0.0411947, 0.042639613, 0.080443196, -0.001779415, 0.050660342, 0.04485121, -0.015451699, 0.043317836, -0.067881316, 0.004261012, -0.0012910204, -0.057707965, 0.042108826, -0.0068448964, 0.0049171196, 0.006922302, 0.041460093, -0.007113974, -0.015525418, 0.057265647, 0.0050866758, 0.026037881, -0.02429809, 0.031227766, -7.3120964E-4, 0.008241889, 0.023354476, -0.012583993, 0.018798584, 0.0019093463, -0.024194883, -0.029974526, 2.6677403E-4, -0.05561432, -0.03857027, -0.013173752, -0.008057589, 0.08168169, 0.062573485, 0.06434276, 0.05900544, 0.046207655, -0.07407379, -0.02170315, -0.058504142, 0.039985694, 0.052783474, -0.012819897, -0.0040914565, -0.057442576, -0.0134244, -0.0075194337, -0.019314624, -0.05502456, -0.058710556, 0.010748367, 0.012333346, -0.014508083, 0.019697968, -0.008993832, -0.020390935, -0.0031201963, 0.06328119, -0.029311046, 0.011559286, 0.01253239, -0.018533193, -0.030844422, -0.0015407467, 0.05735411, 0.06446071, 0.027128937, 0.045735847, 0.023030108, -0.03523813, -0.019580016, -0.005772271, 0.016218387, -0.0087800445, 0.023133317, 0.030667493, -0.100848876, -0.04440889, -0.0077405935, -0.02873603, -0.01848896, -0.06334017, 0.046649978, -0.014028904, 0.006885442, 0.006830152, -0.026863545, 0.02618532, -0.0130558, 0.0049945256, -0.0286918, -0.022145469, -0.010630415, -0.023162805, 0.0066089923, -0.05154498, -0.03450093, -0.03447144, -0.04570636, -0.032230355, -0.0015988011, 0.011802562, 0.06463764, -0.03450093, 0.009863728, -0.007268786, -0.01667545, -0.025713515, -0.040545966, -0.026391737, -0.023914747, -0.019742198, -0.014839823, -0.039012592, 0.021142878, 0.008802161, -0.057442576, 0.033586804, -0.0012117715, -0.016881866, 0.029104631, -0.020818511, 0.02044991, -0.011013758, 0.0019167183, -0.01719149, -0.016985074, 0.0052009416, -0.0229269, -0.015260027, -0.015849786, -0.028072553, -0.02584621, 6.9250667E-4, 0.023605123, -0.040280573, 6.427457E-4, -0.05242962, 0.04399606, -0.010261815, -0.0051898835, 0.024651946, -0.020847999, -1.2866432E-4, 3.0363398E-4, 0.00685964, -0.05110266, 0.010630415, -0.029370023, -0.013276961, -0.023782052, -0.009082296, -0.0052746614, 0.012495529, -0.055172, -0.00685964, -0.0018890734, 0.020361446, -0.022676252, 0.048094887, -0.021098645, 0.0186364, 0.0042388965, 0.046591, -0.024725666, 0.017073538, -0.053225793, 0.016631218, -0.009369804, 0.055407904, 0.04078187, 0.010925295, 0.010335535, 0.03220087, -0.011891026, 0.033586804, -0.016218387, -0.01438276, -0.001853135, -0.03957286, 0.0077848253, -0.022381373, 8.59298E-4, -0.030962374, -0.005602715, 0.028161015, 0.06740951, 0.0073904237, -0.007294588, 0.05531944, 0.027040472, 0.029886063, -0.007471516, 0.01445648, 0.013254845, -5.1327504E-4, 0.008433561, -0.038334366, -9.1228425E-4, 0.017279953, -0.023162805, -0.035916355, 0.028632823, -0.02562505, 0.03317397, 0.047357686, 0.02140827, -0.029414255, -0.0052009416, -0.01386672, -0.0026723477, -0.021201855, 0.0019259333, 0.04234473, -0.011146454, 0.019196672, -0.0049355496, 0.0102323275, -0.00711766, -0.027187912, 0.015127331, 0.009959564, 0.053196307, 0.012119558, -0.006907558, -0.013166381, -0.00471439, -0.033763733, 0.0125471335, -0.04349476, -0.038688224, -0.002272417, -0.00225583, 0.020199263, -0.00654633, 0.0039661326, 0.024180138, -5.450668E-4, -0.0053668115, 0.0055584833, 0.024489762, 0.0077553373, 0.015967738, 0.011714098, 0.003415076, 0.033852194, 0.023501916, 0.052459106, -0.027423816, 0.026391737, -0.01804664, -0.029900806, -0.0290604, -0.041430604, -0.043583225, -0.008131309, -0.014183716, 0.07212759, -0.019697968, -0.0064394367, -4.6213187E-4, -0.062986314, -0.059713148, 0.009959564, -0.004681216, -0.024622459, 0.024076931, -0.014301668, -0.022248676, -0.004795482, 0.049480822, -0.037538193, -0.019167183, 0.046591, 0.027674465, -0.0036417649, -0.017368417, -0.013962556, -0.0029266814, 0.032525238, 0.017088281, 0.010733623, 0.031493157, 0.015142075, 0.049038503, 0.0064615523, -0.012222766, 0.023708332, 0.009443524, 0.0040730266, -0.005624831, -0.052636035, 0.011765702, 5.220293E-4, 0.011198059, 0.011706726, 0.03709587, 0.0079175215, 0.018031897, -0.033557314, 0.009716287, -0.0052119996, -0.015687602, -0.038511295, -0.015053611, -0.009546732, -0.01087369, -0.01579081, -0.0073977956, -0.015952995, -0.010689391, 0.023383964, -0.016292106, 0.03420605, 0.0043531624, 0.008057589, 0.0026041567]";
        return Arrays.stream(arrayEmString
                        .replace("[", "")
                        .replace("]", "")
                        .split(","))
                .map(String::trim)
                .map(Float::parseFloat)
                .toList();
    }


//    @InjectMock
//    ChatService chatService;
//
//    @Inject
//    RetrievalInfoService retrievalInfoService;
//
//    private String testChatId = "test-chat-id";
//
//    @Test
//    void shouldRetryIfThereIsNoUserMessage() {
//        when(chatService.getLastUserMessage(testChatId))
//                .thenThrow(new NotFoundException());
//
//        assertThrows(NotFoundException.class,
//                () -> retrievalInfoService.getMessageId(testChatId));
//
//        // Verifica que foi chamado 3 vezes (1 tentativa + 2 retries)
//        verify(chatService, times(3))
//                .getLastUserMessage(testChatId);
//    }
//
//    @Test
//    void shouldRetrieveUserMessageIfExists() throws InterruptedException {
//        String expectedMessageId = "message-123";
//        when(chatService.getLastUserMessage(testChatId))
//                .thenReturn(new ChatMessageResponse(expectedMessageId, null, null, null));
//
//        String actualMessageId = retrievalInfoService.getMessageId(testChatId);
//
//        assertThat(actualMessageId).isNotNull().isEqualTo(expectedMessageId);
//
//        // Verifica que foi chamado apenas 1 vez
//        verify(chatService, times(1))
//                .getLastUserMessage(testChatId);
//    }


    private void yellowPrint(String text) {
        System.out.println("\u001B[33m" + text + "\u001B[0m");
    }

    private void greenPrint(String text) {
        System.out.println("\u001B[32m" + text + "\u001B[0m");
    }
}

//class ContainsFilter implements Filter {
//
//    private final String key;
//
//    public ContainsFilter(String key) {
//        this.key = ensureNotBlank(key, "key");
//    }
//
//    public String key() {
//        return key;
//    }
//
//    @Override
//    public boolean test(Object object) {
//        if (!(object instanceof Metadata metadata)) {
//            return false;
//        }
//
//        return metadata.containsKey(key);
//    }
//
//    public boolean equals(final Object o) {
//        return false;
//    }
//
//    public int hashCode() {
//        return Objects.hash(key);
//    }
//
//    public String toString() {
//        return "ContainsFilter(key=" + this.key + ")";
//    }
//}