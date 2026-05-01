package com.stridewell.app.ui.main.detail

import com.stridewell.app.model.RunAnalysisResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for [mapAnalysisResult] — the analysis-state mapping that
 * [RunDetailViewModel] runs over the parallel `runAnalysis()` response.
 *
 * Covers all four branches that drive the UI's analysis section:
 *   - success + body → Loaded
 *   - success + null body → NotReady
 *   - 404 → NotReady (analysis not yet computed)
 *   - any other error / exception → Error (rendered silently in UI)
 */
class MapAnalysisResultTest {

    private val sampleResponse = RunAnalysisResponse(
        run_id = "run-1",
        status = "complete",
        computed_at = "2026-04-29T00:00:00Z",
    )

    @Test
    fun `success with body maps to Loaded`() {
        val result = Result.success(Response.success(sampleResponse))

        val state = mapAnalysisResult(result)

        assertTrue(state is RunDetailViewModel.AnalysisState.Loaded)
        assertSame(sampleResponse, (state as RunDetailViewModel.AnalysisState.Loaded).response)
    }

    @Test
    fun `success with null body maps to NotReady`() {
        val response: Response<RunAnalysisResponse> = Response.success(null)
        val result = Result.success(response)

        val state = mapAnalysisResult(result)

        assertEquals(RunDetailViewModel.AnalysisState.NotReady, state)
    }

    @Test
    fun `404 maps to NotReady`() {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val result = Result.success(Response.error<RunAnalysisResponse>(404, errorBody))

        val state = mapAnalysisResult(result)

        assertEquals(RunDetailViewModel.AnalysisState.NotReady, state)
    }

    @Test
    fun `500 maps to Error`() {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val result = Result.success(Response.error<RunAnalysisResponse>(500, errorBody))

        val state = mapAnalysisResult(result)

        assertEquals(RunDetailViewModel.AnalysisState.Error, state)
    }

    @Test
    fun `network exception maps to Error`() {
        val result: Result<Response<RunAnalysisResponse>> =
            Result.failure(java.io.IOException("boom"))

        val state = mapAnalysisResult(result)

        assertEquals(RunDetailViewModel.AnalysisState.Error, state)
    }
}
