package com.example.eeos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eeos.EEOSApplication
import com.example.eeos.consts.AttendStatus
import com.example.eeos.presentation.detail.DetailScreen
import com.example.eeos.presentation.detail.MemberAttendanceViewModel
import com.example.eeos.presentation.detail.ProgramDetailViewModel
import com.example.eeos.presentation.detail.bottomsheet.UserAttendStatusViewModel
import com.example.eeos.presentation.home.HomeScreen
import com.example.eeos.presentation.home.HomeViewModel
import com.example.eeos.presentation.login.LoginScreen
import com.example.eeos.presentation.login.LoginViewModel
import com.example.eeos.presentation.topappbar.TopAppBarViewModel

@Composable
fun EEOSNavGraph(
    code: String?,
    navController: NavHostController = rememberNavController(),
    startDestination: String = EEOSDestinations.LOGIN_ROUTE,
    navActions: EEOSNavigationActions = remember(navController) {
        EEOSNavigationActions(navController)
    },
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(
            EEOSDestinations.LOGIN_ROUTE,
            arguments = listOf(
                navArgument("isLogout") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("isAccountDeleted") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            var isLogout = it.arguments?.getBoolean(EEOSDestinationsArgs.IS_LOGOUT_ARG) ?: false
            var isAccountDeleted = it.arguments?.getBoolean(
                EEOSDestinationsArgs.IS_ACCOUNT_DELETED_ARG
            ) ?: false
            val loginViewModel = hiltViewModel<LoginViewModel>()
            val loginUiState = loginViewModel.loginUiState.collectAsState()

            // 최초 로그인
            if (loginUiState.value.hasTokens) {
                navActions.navigateToHome()
            }

            // 자동 로그인
            if (EEOSApplication.prefs.access != null && EEOSApplication.prefs.refresh != null) {
                navActions.navigateToHome()
            }

            // 로그아웃
            if (isLogout) {
                (loginViewModel::onLogout)() // 왜 두 번 호출 되는지?
                isLogout = false
            }

            // 회원 탈퇴
            if (isAccountDeleted) {
                (loginViewModel::onDeleteAccount)()
                isAccountDeleted = false
            }

            LoginScreen(
                postLogin = { code -> (loginViewModel::postLogin)(code) },
                loginUiState = loginUiState,
                code = code,
            )
        }

        composable(
            EEOSDestinations.HOME_ROUTE,
        ) {
            val topAppBarViewModel = hiltViewModel<TopAppBarViewModel>()
            val homeViewModel = hiltViewModel<HomeViewModel>()

            val topAppBarUiState = topAppBarViewModel.topAppBarUiState.collectAsState()
            val homeUiState = homeViewModel.homeUiState.collectAsState()

            HomeScreen(
                homeUiState = homeUiState,
                topAppBarUiState = topAppBarUiState,
                loadProgramList = { category, programStatus, page ->
                    (homeViewModel::getProgramList)(
                        category,
                        programStatus,
                        page,
                    )
                },
                onProgramClick = { programId -> navActions.navigateToProgramDetail(programId) },
                refreshProgramList = { (homeViewModel::refreshProgramList)() },
                putActiveStatus = { activeStatus ->
                    (topAppBarViewModel::putActiveStatus)(activeStatus)
                },
                onLogoClick = {},
                onLogout = { navActions.navigateToLogin(isLogout = true) },
                onDeleteAccount = {
                    (topAppBarViewModel::deleteUser)()
                },
                onDeleteSuccess = { navActions.navigateToLogin(isAccountDeleted = true) }
            )
        }

        composable(
            EEOSDestinations.DETAIL_ROUTE,
            arguments = listOf(
                navArgument(EEOSDestinationsArgs.PROGRAM_ID_ARG) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) {
            val programId =
                it.arguments?.getInt(EEOSDestinationsArgs.PROGRAM_ID_ARG) ?: 1 // TODO: 에러 처리

            val topAppBarViewModel = hiltViewModel<TopAppBarViewModel>()
            val programDetailViewModel = hiltViewModel<ProgramDetailViewModel>()
            val memberAttendanceViewModel = hiltViewModel<MemberAttendanceViewModel>()
            val userAttendanceViewModel = hiltViewModel<UserAttendStatusViewModel>()

            if (programId == null) {
                // TODO: 에러 처리
            }
            programDetailViewModel.getProgramDetail(programId)
            memberAttendanceViewModel.getAttendeeList(programId, AttendStatus.attend)
            memberAttendanceViewModel.getAttendeeList(programId, AttendStatus.absent)
            memberAttendanceViewModel.getAttendeeList(programId, AttendStatus.late)
            memberAttendanceViewModel.getAttendeeList(programId, AttendStatus.nonResponse)
            userAttendanceViewModel.getUserAttendStatus(programId)

            val topAppBarUiState = topAppBarViewModel.topAppBarUiState.collectAsState()
            val programDetailUiState = programDetailViewModel.detailUiState.collectAsState()
            val memberAttendanceUiState = memberAttendanceViewModel.memberDetailUiState.collectAsState()
            val userAttendanceUiState = userAttendanceViewModel.userAttendStatusUiState.collectAsState()

            DetailScreen(
                detailUiState = programDetailUiState,
                memberUiState = memberAttendanceUiState,
                attendanceUiState = userAttendanceUiState,
                topAppBarUiState = topAppBarUiState,
                putUserAttendStatus = { afterAttendStatus ->
                    (userAttendanceViewModel::putUserAttendStatus)(
                        programId,
                        userAttendanceUiState.value.userAttendStatus,
                        afterAttendStatus,
                    ) {
                        (memberAttendanceViewModel::getAttendeeList)(programId, AttendStatus.attend)
                        (memberAttendanceViewModel::getAttendeeList)(programId, AttendStatus.absent)
                        (memberAttendanceViewModel::getAttendeeList)(programId, AttendStatus.late)
                        (memberAttendanceViewModel::getAttendeeList)(
                            programId,
                            AttendStatus.nonResponse,
                        )
                    }
                },
                putActiveStatus = { activeStatus ->
                    (topAppBarViewModel::putActiveStatus)(activeStatus)
                },
                onLogoClick = { navActions.navigateToHome() },
                onLogout = { navActions.navigateToLogin(isLogout = true) },
                onDeleteAccount = { (topAppBarViewModel::deleteUser)() },
                onDeleteSuccess = { navActions.navigateToLogin(isAccountDeleted = true) }
            )
        }
    }
}
