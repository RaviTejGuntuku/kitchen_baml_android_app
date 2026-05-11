client<llm> PlannerClient {
  provider openai-generic
  options {
    model "openai/gpt-4.1-mini"
    api_key env.OPENROUTER_API_KEY
    base_url "https://openrouter.ai/api"
    temperature 0.2
  }
}
