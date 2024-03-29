package com.example.eeos.data.repository

import com.example.eeos.data.model.remote.response.ResponsePostLoginDto
import com.example.eeos.data.model.remote.response.ResponseReIssueTokenDto
import com.example.eeos.data.model.remote.response.base.BaseResponse
import com.example.eeos.data.source.AuthDataSource
import com.example.eeos.domain.repository.AuthRepository
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: AuthDataSource
) : AuthRepository {
    override suspend fun postLogin(
        code: String
    ): ApiResponse<BaseResponse<ResponsePostLoginDto>> =
        authDataSource.postLogin(code)

    override suspend fun reIssueToken(
        refreshToken: String
    ): Result<ResponseReIssueTokenDto> =
        runCatching {
            authDataSource.reIssueToken(refreshToken).data!!
        }
}
