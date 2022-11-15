package me.kamelajda.valorantrpc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class LockFileData {

    private String name;
    private String pid;
    private int port;
    private String base64Password;
    private String password;
    private String protocol;

}
