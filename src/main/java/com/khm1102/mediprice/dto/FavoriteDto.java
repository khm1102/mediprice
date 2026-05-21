package com.khm1102.mediprice.dto;

public record FavoriteDto(
        String ykiho,
        String hospitalName,
        String address,
        String clCdNm,
        String telNo,
        long favoritedAt,
        Double lat,
        Double lng
) {
}
