// Chat types based on api_chat.md
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatRequest {
  currentMessage: string;
  chatHistory: ChatMessage[];
}

export interface MetricsUsed {
  bodyBattery?: number;
  sleepScore?: number;
  trainingLoad48h?: string;
}

export interface ChatResponse {
  reply: string;
  metricsUsed: MetricsUsed;
}

export interface ErrorResponse {
  error: string;
  message: string;
}

// Dashboard types
export interface HealthMetrics {
  sleepScore: number;
  bodyBattery: number;
}

export interface NutritionMetrics {
  protein: number;
  carbs: number;
  fat: number;
  calories: number;
  caloriesRemaining: number;
}

export interface TrainingMetrics {
  nextWorkout: string;
}
