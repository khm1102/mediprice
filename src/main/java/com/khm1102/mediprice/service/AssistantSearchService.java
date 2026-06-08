package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.AssistantHospitalSearchResponse;
import com.khm1102.mediprice.dto.AssistantMatchedItemDto;
import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.repository.NonPayItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AssistantSearchService {

    private static final int MAX_MATCHED_ITEMS = 20;
    private static final double DEFAULT_W_PRICE = 0.7;
    private static final double DEFAULT_W_DISTANCE = 0.3;
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final List<String> STOPWORDS = List.of(
            "추천해줘", "찾아줘", "저렴하게", "저렴한", "가까운",
            "병원", "의원", "찾아", "추천", "검색",
            "근처", "주변", "가까이", "싸게", "싼", "저렴",
            "가격", "비용", "에서"
    );
    private static final Set<String> TOKEN_STOPWORDS = Set.of("곳", "데");

    private final NonPayItemRepository nonPayItemRepository;
    private final HospitalService hospitalService;

    public AssistantSearchService(NonPayItemRepository nonPayItemRepository,
                                  HospitalService hospitalService) {
        this.nonPayItemRepository = nonPayItemRepository;
        this.hospitalService = hospitalService;
    }

    public AssistantHospitalSearchResponse search(String query,
                                                  double lat,
                                                  double lng,
                                                  int radius,
                                                  String sort,
                                                  int limit) {
        String normalizedQuery = normalize(query);
        String searchKeyword = toSearchKeyword(normalizedQuery);
        String interpretedSort = resolveSort(sort, normalizedQuery);
        List<AssistantMatchedItemDto> matchedItems = nonPayItemRepository
                .searchNaturalLanguageMatches(searchKeyword, MAX_MATCHED_ITEMS)
                .stream()
                .map(AssistantSearchService::toDto)
                .toList();

        if (matchedItems.isEmpty()) {
            return new AssistantHospitalSearchResponse(
                    normalizedQuery,
                    interpretedSort,
                    List.of(),
                    "'" + searchKeyword + "'와 가까운 비급여 항목을 찾지 못했어요.",
                    List.of());
        }

        List<String> npayCds = matchedItems.stream()
                .map(AssistantMatchedItemDto::npayCd)
                .filter(code -> code != null && !code.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        List<HospitalSummaryDto> hospitals = hospitalService.searchNearbyV2(
                lat, lng, npayCds, radius, interpretedSort, limit, DEFAULT_W_PRICE, DEFAULT_W_DISTANCE);
        return new AssistantHospitalSearchResponse(
                normalizedQuery,
                interpretedSort,
                matchedItems,
                buildMessage(searchKeyword, matchedItems, interpretedSort),
                hospitals);
    }

    static String resolveSort(String requestedSort, String query) {
        if (requestedSort != null && HospitalService.ALLOWED_SORTS.contains(requestedSort)) {
            return requestedSort;
        }
        String normalized = normalize(query);
        if (containsAny(normalized, Set.of("싼", "저렴", "가격", "비용"))) {
            return "price";
        }
        if (containsAny(normalized, Set.of("가까운", "근처", "주변", "가까이"))) {
            return "distance";
        }
        return "mixed";
    }

    static String toSearchKeyword(String query) {
        String keyword = normalize(query);
        for (String stopword : STOPWORDS) {
            keyword = keyword.replace(stopword, " ");
        }
        keyword = SPACE_PATTERN.splitAsStream(normalize(keyword))
                .filter(token -> !TOKEN_STOPWORDS.contains(token))
                .collect(java.util.stream.Collectors.joining(" "));
        return keyword.isBlank() ? normalize(query) : keyword;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return SPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
    }

    private static boolean containsAny(String value, Set<String> needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static AssistantMatchedItemDto toDto(NonPayItemRepository.SearchMatch match) {
        return new AssistantMatchedItemDto(
                match.getNpayCd(),
                match.getNpayKorNm(),
                match.getNpayMdivCdNm(),
                match.getNpaySdivCdNm(),
                match.getScore());
    }

    private static String buildMessage(String keyword,
                                       List<AssistantMatchedItemDto> matchedItems,
                                       String interpretedSort) {
        String itemName = matchedItems.getFirst().npayKorNm();
        String sortLabel = switch (interpretedSort) {
            case "price" -> "가격 우선";
            case "distance" -> "거리 우선";
            default -> "가격과 거리 혼합";
        };
        return "'" + keyword + "'와 가까운 '" + itemName + "' 기준으로 " + sortLabel + " 검색했어요.";
    }
}
