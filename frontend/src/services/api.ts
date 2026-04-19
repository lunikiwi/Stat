import type { ChatRequest, ChatResponse, ErrorResponse, MetricsResponse, NutritionLogRequest, NutritionEntry } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  statusCode: number;
  errorResponse?: ErrorResponse;

  constructor(
    message: string,
    statusCode: number,
    errorResponse?: ErrorResponse
  ) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.errorResponse = errorResponse;
  }
}

export async function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const errorData: ErrorResponse = await response.json();
      throw new ApiError(
        errorData.message || 'Failed to send message',
        response.status,
        errorData
      );
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error: Could not reach the server', 0);
  }
}

export async function logNutrition(request: NutritionLogRequest): Promise<void> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/nutrition/log`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const errorData: ErrorResponse = await response.json();
      throw new ApiError(
        errorData.message || 'Failed to log nutrition',
        response.status,
        errorData
      );
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error: Could not reach the server', 0);
  }
}

export async function fetchMetrics(): Promise<MetricsResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/metrics`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorData: ErrorResponse = await response.json();
      throw new ApiError(
        errorData.message || 'Failed to fetch metrics',
        response.status,
        errorData
      );
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error: Could not reach the server', 0);
  }
}

export async function fetchNutritionEntries(): Promise<NutritionEntry[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/nutrition/entries`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorData: ErrorResponse = await response.json();
      throw new ApiError(
        errorData.message || 'Failed to fetch nutrition entries',
        response.status,
        errorData
      );
    }

    return await response.json();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error: Could not reach the server', 0);
  }
}

export async function deleteNutritionEntry(timestamp: string): Promise<void> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/nutrition/entries/${encodeURIComponent(timestamp)}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorData: ErrorResponse = await response.json();
      throw new ApiError(
        errorData.message || 'Failed to delete nutrition entry',
        response.status,
        errorData
      );
    }
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error: Could not reach the server', 0);
  }
}
