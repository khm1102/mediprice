package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.FavoriteDto;
import com.khm1102.mediprice.entity.Favorite;
import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.global.exception.business.FavoriteAlreadyExistsException;
import com.khm1102.mediprice.global.exception.business.FavoriteNotFoundException;
import com.khm1102.mediprice.repository.FavoriteRepository;
import com.khm1102.mediprice.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final HospitalRepository hospitalRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           HospitalRepository hospitalRepository) {
        this.favoriteRepository = favoriteRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /** 즐겨찾기 조회 */
    @Transactional(readOnly = true)
    public List<FavoriteDto> lookupFavorites(Long memberId) {
        List<Favorite> favorites = favoriteRepository.findByMemberIdAndDeletedDttmIsNull(memberId);

        return favorites.stream()
                .map(f -> {
                    Hospital h = hospitalRepository.findById(f.getYkiho()).orElse(null);
                    return new FavoriteDto(
                            f.getYkiho(),
                            h != null ? h.getYadmNm() : "알 수 없음",
                            h != null ? h.getAddr() : "",
                            h != null ? h.getClCdNm() : "",
                            h != null ? h.getTelNo() : "",
                            f.getCreatedDttm().toEpochSecond(),
                            h != null ? h.getYPos() : null,
                            h != null ? h.getXPos() : null
                    );
                })
                .toList();
    }

    /** 즐겨찾기 추가 */
    public void addFavorite(Long memberId, String ykiho) {
        favoriteRepository.findByMemberIdAndYkiho(memberId, ykiho).ifPresentOrElse(
                existing -> {
                    if (existing.getDeletedDttm() == null) {
                        throw new FavoriteAlreadyExistsException();
                    }
                    existing.restore();
                },
                () -> favoriteRepository.save(Favorite.create(memberId, ykiho))
        );
    }

    /** 즐겨찾기 제거 */
    public void removeFavorite(Long memberId, String ykiho) {
        Favorite favorite = favoriteRepository
                .findByMemberIdAndYkihoAndDeletedDttmIsNull(memberId, ykiho)
                .orElseThrow(FavoriteNotFoundException::new);
        favorite.delete();
    }

    @Transactional(readOnly = true)
    public boolean existsFavorite(Long memberId, String ykiho) {
        return favoriteRepository.existsByMemberIdAndYkihoAndDeletedDttmIsNull(memberId, ykiho);
    }
}
