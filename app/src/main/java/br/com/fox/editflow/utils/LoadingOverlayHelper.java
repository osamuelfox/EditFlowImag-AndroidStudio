package br.com.fox.editflow.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import br.com.fox.editflow.R;

/**
 * Helper estático para exibir e ocultar o LoadingOverlay reutilizável.
 *
 * <p>O overlay é incluído via {@code <include>} no layout de cada Activity com id
 * {@code @+id/loadingOverlay}. Este helper controla sua visibilidade e mensagem,
 * além de garantir que eventos de toque sejam interceptados enquanto ativo.
 *
 * <p>Uso típico:
 * <pre>
 *   // Exibir
 *   LoadingOverlayHelper.show(binding.loadingOverlay, "Carregando…");
 *
 *   // Ocultar
 *   LoadingOverlayHelper.hide(binding.loadingOverlay);
 * </pre>
 */
public final class LoadingOverlayHelper {

    private LoadingOverlayHelper() {
        // Utilitário estático — não instanciar
    }

    /**
     * Exibe o overlay com a mensagem contextual fornecida.
     *
     * @param overlay   A view raiz do overlay (id: loadingOverlay)
     * @param message   Mensagem a exibir abaixo do spinner (ex: "Enviando imagem…")
     */
    public static void show(View overlay, String message) {
        if (overlay == null) return;

        TextView tvMessage = overlay.findViewById(R.id.overlayMessage);
        if (tvMessage != null && message != null) {
            tvMessage.setText(message);
        }

        if (overlay.getVisibility() != View.VISIBLE) {
            Animation fadeIn = AnimationUtils.loadAnimation(overlay.getContext(), R.anim.fade_in);
            overlay.startAnimation(fadeIn);
            overlay.setVisibility(View.VISIBLE);
        }

        // Garante intercepção de toque
        overlay.setClickable(true);
        overlay.setFocusable(true);
    }

    /**
     * Oculta o overlay com animação de fade out.
     *
     * @param overlay A view raiz do overlay (id: loadingOverlay)
     */
    public static void hide(View overlay) {
        if (overlay == null || overlay.getVisibility() != View.VISIBLE) return;

        Animation fadeOut = AnimationUtils.loadAnimation(overlay.getContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                overlay.setVisibility(View.GONE);
            }
        });
        overlay.startAnimation(fadeOut);
    }

    /**
     * Desabilita visualmente um conjunto de views durante o loading
     * (alpha 0.5, isEnabled = false).
     *
     * @param enabled true para reabilitar, false para desabilitar
     * @param views   As views a controlar (campos, botões, etc.)
     */
    public static void setFormEnabled(boolean enabled, View... views) {
        float alpha = enabled ? 1.0f : 0.5f;
        for (View v : views) {
            if (v != null) {
                v.setEnabled(enabled);
                v.setAlpha(alpha);
            }
        }
    }
}
