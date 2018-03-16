package org.tron.explorer.controller;


import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.tron.common.utils.ByteArray;
import org.tron.explorer.domain.AccountVo;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;
import org.tron.walletserver.WalletClient;


@RestController
public class GrpcClientController {

  protected final Log log = LogFactory.getLog(getClass());


  @GetMapping("/")
  public ModelAndView viewIndex() {
    return new ModelAndView("index");
  }

  @GetMapping("/queryAccount")
  public ModelAndView viewqueryAccount() {
    return new ModelAndView("accountList");
  }


  @GetMapping("/myproto")
  public ModelAndView viewMyproto() {
    return new ModelAndView("myproto");
  }

  @ModelAttribute
  AccountVo setAccountVo() {
    return new AccountVo();
  }


  @ApiOperation(value = "get Balance", notes = "query balance")
  @ApiImplicitParam(name = "address", value = "address", required = true, dataType = "String")
  @PostMapping("/balance")
  public ModelAndView getBalance(@ModelAttribute AccountVo accountVo) {

    long balance = WalletClient.getBalance(ByteArray.fromHexString(accountVo.getAddress()));
    ModelAndView modelAndView = new ModelAndView("balance");
    modelAndView.addObject("address", accountVo.getAddress());
    modelAndView.addObject("balance", balance);
    return modelAndView;
  }

  @GetMapping("/accountList")
  public byte[] getAcountList()
      throws IOException {

    List<Account> accountsList = WalletClient.listAccounts().get().getAccountsList();

    int accountsSize = 0;
    for (int i = 0; i < accountsList.size(); i++) {
      Account account = accountsList.get(i);
      accountsSize += account.getSerializedSize();
      accountsSize += 2;  //Length
    }

    byte[] returnBytes = new byte[accountsSize];

    accountsSize = 0;
    for (int i = 0; i < accountsList.size(); i++) {
      Account account = accountsList.get(i);
      byte[] accountsBytes = account.toByteArray();
      int length = accountsBytes.length;
      returnBytes[accountsSize++] = (byte) ((length & 0xFFFF) >> 8);
      returnBytes[accountsSize++] = (byte) (length & 0xFF);
      System.arraycopy(accountsBytes, 0, returnBytes, accountsSize, length);
      accountsSize += length;
    }

    return returnBytes;
  }

  @GetMapping("/alTest")
  public  byte[] getAcountListForTest()
      throws IOException {

    final List<Account> accountsList = WalletClient.listAccounts().get().getAccountsList();


    Account account = accountsList.get(0);

    final Encoder encoder = Base64.getEncoder();
    byte[] accountsBytes = account.toByteArray();
    final byte[] encode = encoder.encode(accountsBytes);

    String   encodeString = new String(encode,"ISO-8859-1");

    System.out.println("Name::: " + ByteArray.toHexString(account.getAccountName().toByteArray()));
    System.out.println("Address::: " + ByteArray.toHexString(account.getAddress().toByteArray()));
    System.out.println("Balance::: " + account.getBalance());

    System.out.println("base64String::: " + encodeString);


    return  accountsBytes;
  }



  @GetMapping("/aTest")
  public String getAcountForTest() {

    final List<Account> accountsList = WalletClient.listAccounts().get().getAccountsList();

    final JsonFormat jsonFormat = new JsonFormat();
    List list = new ArrayList();
    for (Account account : accountsList) {
      final String accountStr = ByteArray.toHexString(account.getAddress().toByteArray());

      final String s = jsonFormat.printToString(account);
      list.add(s);
      System.out.println("s :" + s);

    }
    return list.toString();

  }

  @GetMapping("/witnessList")
  public ModelAndView getWitnessList() {

    List<Witness> witnessList = WalletClient.listWitnesses().get().getWitnessesList();
    ModelAndView modelAndView = new ModelAndView("witnessList");
    witnessList.forEach(witness -> {
    });

    System.out
        .println("Address " + ByteArray.toHexString(witnessList.get(0).getAddress().toByteArray()));
    System.out
        .println("PubKey " + ByteArray.toHexString(witnessList.get(0).getPubKey().toByteArray()));

    modelAndView.addObject("witnessList", witnessList);

    return modelAndView;
  }

  @PostMapping("/register")
  public ModelAndView registerAccount(@ModelAttribute AccountVo account) {
    ModelAndView modelAndView;
    try {
      Transaction transaction = WalletClient
          .createAccountTransaction(AccountType.Normal, account.getName().getBytes(),
              ByteArray.fromHexString(account.getAddress()));
      Any contract = transaction.getRawData().getContract(0).getParameter();
      AccountCreateContract accountCreateContract = contract.unpack(AccountCreateContract.class);
      modelAndView = new ModelAndView("register");
      modelAndView.addObject("name",
          new String(accountCreateContract.getAccountName().toByteArray(), "ISO-8859-1"));
      modelAndView.addObject("address",
          ByteArray.toHexString(accountCreateContract.getOwnerAddress().toByteArray()));
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      modelAndView = new ModelAndView("error");
      modelAndView.addObject("message", "invalid transaction!!!");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      modelAndView = new ModelAndView("error");
      modelAndView.addObject("message", "invalid transaction!!!");
    }
    return modelAndView;
  }
}
