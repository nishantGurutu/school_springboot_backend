package com.school.management.service;

import com.school.management.dto.MasterDataResponse;
import com.school.management.entity.UserEntity;

public interface MasterDataService {

    /**
     * Get master data for currently authenticated user.
     * Contains complete userDetails (all fields added for teacher/staff/student/parent) and future settings.
     *
     * @param currentUser currently authenticated UserEntity
     * @return MasterDataResponse
     */
    MasterDataResponse getMasterData(UserEntity currentUser);
}
