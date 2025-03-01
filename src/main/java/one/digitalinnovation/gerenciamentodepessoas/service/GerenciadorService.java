package one.digitalinnovation.gerenciamentodepessoas.service;

import one.digitalinnovation.gerenciamentodepessoas.model.*;
import one.digitalinnovation.gerenciamentodepessoas.repository.*;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class GerenciadorService {

    @Autowired
    private GerenciadorRepository gerenciadorRepository;
    @Autowired
    private FuncionarioRepository funcionarioRepository;

    public Optional<Gerenciador> cadastroGerenciador(Gerenciador gerenciador){
        if (gerenciadorRepository.findByEmail(gerenciador.getEmail()).isPresent())
            return Optional.empty();
        gerenciador.setSenha(criptografaSenha(gerenciador.getSenha()));
        return Optional.of(gerenciadorRepository.save(gerenciador));
    }

    public Optional<Funcionario> cadastroFuncionario(Funcionario funcionario){
        constroiFuncionario(funcionario);
        if (funcionarioRepository.findByCredencial(funcionario.getCredencial()).isPresent())
            return Optional.empty();
        return Optional.of(funcionarioRepository.save(funcionario));
    }

    public Optional<Gerenciador> atualizarGerenciador(Gerenciador gerenciador){
        if (gerenciadorRepository.findById(gerenciador.getId()).isPresent()){
            return compararGerenciador(gerenciador);
        }
        return Optional.empty();
    }

    public Optional<Funcionario> atualizarFuncionario(Funcionario funcionario) {
        if (gerenciadorRepository.findById(funcionario.getId()).isPresent()){
            return compararFuncionario(funcionario);
        }
        return Optional.empty();
    }


    public Optional<GerenciadorLogin> autenticarGerenciador(GerenciadorLogin gerenciadorLogin){
        Optional<Gerenciador> gerenciador = gerenciadorRepository.findByEmail(gerenciadorLogin.getEmail());
        if(gerenciador.isPresent()){
            if(compararSenhas(gerenciadorLogin.getSenha(), gerenciador.get().getSenha())){
                String token = gerarBasicToken(gerenciador.get().getEmail(), gerenciadorLogin.getSenha());
                gerenciadorLogin.setId(gerenciador.get().getId());
                gerenciadorLogin.setNome(gerenciador.get().getNome());
                gerenciadorLogin.setSenha(gerenciador.get().getSenha());
                gerenciadorLogin.setToken(token);
                return Optional.of(gerenciadorLogin);
            }
        }
        return Optional.empty();
    }

    /*
    * Inicio dos métodos privados
    */

    private String gerarBasicToken(String usuario, String senha){
        String tokenBase = usuario + ":" + senha;
        byte[] tokenBase64 = Base64.encodeBase64(tokenBase.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(tokenBase64);
    }

    private String criptografaSenha(String senha){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(senha);
    }

    private Optional<Gerenciador> compararGerenciador(Gerenciador gerenciador){
        var gerenciadorOp = gerenciadorRepository.findByEmail(gerenciador.getEmail());
        var isPresent = gerenciadorOp.isPresent();
        if (isPresent && gerenciador.getId() != gerenciadorOp.get().getId()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O Gerenciador já existe!", null);
        }
        gerenciador.setSenha(criptografaSenha(gerenciador.getSenha()));
        return Optional.of(gerenciadorRepository.save(gerenciador));
    }

    private Optional<Funcionario> compararFuncionario(Funcionario funcionario) {
        Optional<Funcionario> funcionarioOp = funcionarioRepository.findByCredencial(funcionario.getCredencial());
        var isPresent = funcionarioOp.isPresent();
        if (isPresent && funcionario.getId() != funcionarioOp.get().getId()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "O Funcionario já existe!", null);
        }
        return Optional.of(funcionarioRepository.save(funcionario));
    }

    private void constroiFuncionario(Funcionario funcionario) {
        setor(funcionario);
        String credencial = geraCredencial(funcionario);
        System.out.println(credencial);
        funcionario.setCredencial(credencial);
    }

    private boolean compararSenhas(String senhaDigitada, String senhaDoBanco){
        var encoder = new BCryptPasswordEncoder();
        return encoder.matches(senhaDigitada,senhaDoBanco);
    }

    private String geraCredencial(Funcionario funcionario){
        String random = String.valueOf((int)(Math.random() * 99));
        return "S-"
                + funcionario.getSetor() + ":"
                + funcionario.getNome().toLowerCase()
                + "-"
                + random;
    }
    private void setor(Funcionario funcionario) {
        Optional<Gerenciador> gerenciador = gerenciadorRepository.findById(funcionario.getGerenciador().getId());
        int setor = gerenciador.get().getSetor();
        funcionario.setSetor(setor);
    }
}