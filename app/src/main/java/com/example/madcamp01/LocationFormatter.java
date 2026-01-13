package com.example.madcamp01;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 위치 정보를 포맷팅하는 유틸리티 클래스
 * HeroPostAdapter와 PostAdapter에서 중복된 위치 포맷팅 로직을 통합
 */
public class LocationFormatter {

    public static final String DEFAULT_LOCATION = "어딘가에서";

    /**
     * DocumentSnapshot에서 cities와 countries 배열을 읽어서 예쁘게 포맷팅
     * 
     * @param snapshot DocumentSnapshot (cities와 countries 필드 포함)
     * @return 포맷팅된 위치 문자열 (예: "Seoul, South Korea, Tokyo, Japan" 또는 "Seoul, South Korea")
     */
    public static String formatLocationFromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null) {
            return DEFAULT_LOCATION;
        }

        List<String> cities = (List<String>) snapshot.get("cities");
        List<String> countries = (List<String>) snapshot.get("countries");

        // 중복 제거 및 정리
        Set<String> uniqueCities = removeEmptyAndTrim(cities);
        Set<String> uniqueCountries = removeEmptyAndTrim(countries);

        // 위치 정보가 없으면 기본값 반환
        if (uniqueCities.isEmpty() && uniqueCountries.isEmpty()) {
            return DEFAULT_LOCATION;
        }

        // 도시와 국가를 조합하여 위치 정보 생성
        List<String> locationParts = combineCitiesAndCountries(uniqueCities, uniqueCountries);

        // 위치 정보 포맷팅
        return formatLocationParts(locationParts);
    }

    /**
     * PostItem의 imageLocations에서 위치 정보를 가공 (fallback)
     * 
     * @param item PostItem 객체
     * @return 포맷팅된 위치 문자열
     */
    public static String formatLocationFromImageLocations(PostItem item) {
        if (item == null) {
            return DEFAULT_LOCATION;
        }

        List<String> imageLocations = item.getImageLocations();
        if (imageLocations == null || imageLocations.isEmpty()) {
            return DEFAULT_LOCATION;
        }

        Set<String> uniqueLocations = removeEmptyAndTrim(imageLocations);

        if (uniqueLocations.isEmpty()) {
            return DEFAULT_LOCATION;
        }

        List<String> locationList = new ArrayList<>(uniqueLocations);
        return formatLocationParts(locationList);
    }

    /**
     * 리스트에서 빈 문자열과 null을 제거하고 trim 처리
     * "알 수 없는", "정보 없음" 같은 값도 필터링
     */
    private static Set<String> removeEmptyAndTrim(List<String> list) {
        Set<String> result = new HashSet<>();
        if (list != null) {
            for (String item : list) {
                if (item != null && !item.trim().isEmpty()) {
                    String trimmed = item.trim();
                    // "알 수 없는", "정보 없음" 같은 값 필터링
                    if (!isInvalidLocationText(trimmed)) {
                        result.add(trimmed);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 유효하지 않은 위치 텍스트인지 확인
     */
    private static boolean isInvalidLocationText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        String lower = text.toLowerCase();
        return lower.contains("알 수 없") || 
               lower.contains("정보 없음") || 
               lower.contains("정보없음") ||
               lower.contains("unknown");
    }

    /**
     * 도시와 국가를 조합하여 위치 정보 리스트 생성
     */
    private static List<String> combineCitiesAndCountries(Set<String> uniqueCities, Set<String> uniqueCountries) {
        List<String> locationParts = new ArrayList<>();
        List<String> cityList = new ArrayList<>(uniqueCities);
        List<String> countryList = new ArrayList<>(uniqueCountries);

        // 도시와 국가를 매칭하여 위치 문자열 생성
        if (!cityList.isEmpty() && !countryList.isEmpty()) {
            // 도시와 국가가 모두 있는 경우
            int maxSize = Math.max(cityList.size(), countryList.size());
            for (int i = 0; i < maxSize; i++) {
                String city = i < cityList.size() ? cityList.get(i) : cityList.get(0);
                String country = i < countryList.size() ? countryList.get(i) : countryList.get(0);
                locationParts.add(city + ", " + country);
            }
        } else if (!cityList.isEmpty()) {
            // 도시만 있는 경우
            locationParts.addAll(cityList);
        } else if (!countryList.isEmpty()) {
            // 국가만 있는 경우
            locationParts.addAll(countryList);
        }

        return locationParts;
    }

    /**
     * 위치 정보 리스트를 포맷팅
     * - 1개: "Seoul, South Korea"
     * - 2개: "Seoul, South Korea, Tokyo, Japan"
     * - 3개 이상: "Seoul, South Korea, Tokyo, Japan 외 2곳"
     */
    private static String formatLocationParts(List<String> locationParts) {
        if (locationParts == null || locationParts.isEmpty()) {
            return DEFAULT_LOCATION;
        }

        if (locationParts.size() == 1) {
            return locationParts.get(0);
        } else if (locationParts.size() == 2) {
            return locationParts.get(0) + ", " + locationParts.get(1);
        } else {
            // 3개 이상일 때는 첫 두 개와 개수 표시
            return locationParts.get(0) + ", " + locationParts.get(1) + " 외 " + (locationParts.size() - 2) + "곳";
        }
    }
}
