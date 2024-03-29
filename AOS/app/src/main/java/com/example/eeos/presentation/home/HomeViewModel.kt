package com.example.eeos.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eeos.consts.programCategory
import com.example.eeos.consts.programStatus
import com.example.eeos.domain.model.Program
import com.example.eeos.domain.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class HomeUiState(
    val isEmpty: Boolean = false,
    val isLoading: Boolean = false,
    val programList: List<Program>? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    ) : ViewModel() {
        private val _homeUiState = MutableStateFlow(HomeUiState())
        val homeUiState = _homeUiState.asStateFlow()

    init {
        getProgramList(
            category = programCategory[0],
            programStatus = programStatus[0],
            page = 0
        )
    }

    fun getProgramList(category: String, programStatus: String, page: Int) {
        viewModelScope.launch {
            programRepository.getProgramList(
                category = category,
                programStatus = programStatus,
                page = page,
                size = 7
            )
                .onSuccess { response ->
                    val programList: MutableList<Program> = mutableListOf()
                    val originalList = homeUiState.value.programList

                    if (originalList != null) {
                        programList.addAll(originalList)
                        programList.addAll(response)
                    } else {
                        programList.addAll(response)
                    }

                    _homeUiState.update { currentState ->
                        currentState.copy(programList = programList)
                    }
                }
                .onFailure { exception ->
                    when (exception) {
                        is HttpException -> {
                            _homeUiState.update { currentState ->
                                currentState.copy(
                                    isEmpty = true
                                )
                            }
                        }

                        is IOException -> {
                            _homeUiState.update { currentState ->
                                currentState.copy(
                                    isEmpty = true
                                )
                            }
                        }
                    }
                }
        }
    }

    fun refreshProgramList() {
        _homeUiState.update { currentState ->
            currentState.copy(programList = null)
        }
    }
}
