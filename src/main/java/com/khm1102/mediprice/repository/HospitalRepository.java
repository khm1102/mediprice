package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, String> {

    /** Hospital.location GEOGRAPHY 컬럼을 native UPDATE로 채움. JTS Point 매핑 회피. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE Hospital SET location = ST_MakePoint(:xPos, :yPos)::geography "
            + "WHERE ykiho = :ykiho", nativeQuery = true)
    int updateLocation(@Param("ykiho") String ykiho,
                       @Param("xPos") double xPos,
                       @Param("yPos") double yPos);

    /**
     * PostGIS 프로시저 호출 — JSON 문자열 반환. Service에서 파싱.
     * <p>
     * {@code ::text} 캐스팅 — Hibernate 7이 JSON 반환 타입에 FormatMapper 필요로 하는데
     * 우리는 Service에서 직접 JsonMapper로 파싱하므로 String으로 받는다.
     */
    @Query(value = "SELECT search_nearby_hospitals(:lat, :lng, :npayCd, :radius)::text", nativeQuery = true)
    String searchNearbyJson(@Param("lat") double lat,
                            @Param("lng") double lng,
                            @Param("npayCd") String npayCd,
                            @Param("radius") int radius);

    /** PriceSyncService가 ykiho별 가격상세를 호출할 때 사용. */
    @Query("SELECT h.ykiho FROM Hospital h")
    List<String> findAllYkiho();
}
