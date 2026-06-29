package br.com.fox.editflow.ui;

/**
 * Sealed-class equivalente em Java para representar o estado da UI.
 * <p>
 * Uso com LiveData:
 * <pre>
 *   MutableLiveData&lt;UiState&gt; state = new MutableLiveData&lt;&gt;(new UiState.Idle());
 *   state.observe(this, s -> {
 *       if (s instanceof UiState.Loading) { ... }
 *       else if (s instanceof UiState.Success) { ... }
 *       else if (s instanceof UiState.Error) { ... }
 *   });
 * </pre>
 */
public abstract class UiState {

    /** Estado inicial — nenhuma operação em andamento. */
    public static final class Idle extends UiState {
        public Idle() {}
    }

    /** Operação em andamento — exibe LoadingOverlay com mensagem contextual. */
    public static final class Loading extends UiState {
        public final String message;

        public Loading(String message) {
            this.message = message;
        }
    }

    /** Operação concluída com sucesso. Carrega dados genéricos opcionais. */
    public static final class Success<T> extends UiState {
        public final T data;

        public Success(T data) {
            this.data = data;
        }

        public Success() {
            this.data = null;
        }
    }

    /** Operação falhou — exibe erro inline ou Snackbar. */
    public static final class Error extends UiState {
        public final String message;
        /** Se true, a UI deve oferecer um botão "Tentar Novamente". */
        public final boolean retryable;

        public Error(String message, boolean retryable) {
            this.message = message;
            this.retryable = retryable;
        }

        public Error(String message) {
            this(message, true);
        }
    }
}
