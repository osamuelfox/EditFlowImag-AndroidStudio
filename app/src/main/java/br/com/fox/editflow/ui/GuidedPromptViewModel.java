package br.com.fox.editflow.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GuidedPromptViewModel extends ViewModel {
    public final MutableLiveData<String> style = new MutableLiveData<>("");
    public final MutableLiveData<String> platform = new MutableLiveData<>("");
    public final MutableLiveData<String> product = new MutableLiveData<>("");
    public final MutableLiveData<String> tone = new MutableLiveData<>("");

    public String buildPrompt() {
        return String.format("Edite esta imagem no estilo %s, otimizada para %s, destacando o produto/serviço %s, com tom %s.",
                style.getValue(), platform.getValue(), product.getValue(), tone.getValue());
    }

    public boolean isComplete() {
        return !style.getValue().isEmpty() &&
               !platform.getValue().isEmpty() &&
               !product.getValue().isEmpty() &&
               !tone.getValue().isEmpty();
    }
}
