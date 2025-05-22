package bitcamp.project1.command;

import bitcamp.project1.Prompt.Prompt;
import bitcamp.project1.util.ArrayList;
import bitcamp.project1.vo.Finance;
import bitcamp.project1.vo.Wallet;

import java.time.LocalDate;

public class IncomeCommand {
  private final int PROCESS_LIST = 0;
  private final int PROCESS_SEARCH = 1;
  private final int PROCESS_UPDATE = 2;
  private final int PROCESS_DELETE = 3;

  ArrayList incomeList = new ArrayList();
  Object[] wallet;

  public IncomeCommand(Object[] list) {
    wallet = list;
  }

  public void autoIncomeData() {
    Finance income = new Finance();
    income.setKindOfCome("입금");
    income.setDate(LocalDate.parse("2024-06-27"));
    income.setCategory("월급");
    income.setAccount(1);
    income.setAmount(50000);
    income.setNo(Finance.getSeqNo());
    incomeList.add(income);

    Finance income1 = new Finance();
    income1.setKindOfCome("입금");
    income1.setDate(LocalDate.parse("2024-06-28"));
    income1.setCategory("용돈");
    income1.setAccount(0);
    income1.setAmount(3000);
    income1.setNo(Finance.getSeqNo());
    incomeList.add(income1);
  }

  public void executeIncomeCommand(String subTitle) {
    System.out.printf("[%s]\n", subTitle);
    switch (subTitle) {
      case "등록":
        createIncome();
        break;
      case "목록":
        listIncome();
        break;
      case "조회":
        searchIncome();
        break;
      case "변경":
        updateIncome();
        break;
      case "삭제":
        deleteIncome();
        break;
      default:
    }
  }

  private void createIncome() {
    Finance income = new Finance();
    income.setKindOfCome("수입");
    income.setDate(Prompt.inputDate("수입일(yyyy-MM-dd)?"));
    income.setAmount(Prompt.inputInt("수입금액?"));
    setWallet(income);
    income.setCategory(Prompt.input("카테고리?"));
    income.setNo(Finance.getSeqNo());
    incomeList.add(income);
  }

  private void listIncome() {
    System.out.println("날짜 항목 금액");
    printNoList(PROCESS_LIST);
  }

  private void searchIncome() {
    printNoList(PROCESS_SEARCH);
    int incomeNo = Prompt.inputInt("조회 할 수입 번호?");
    Finance searchedIncome = (Finance) incomeList.get(incomeList.indexOf(new Finance(incomeNo)));
    if (searchedIncome == null) {
      System.out.println("없는 수입 번호입니다.");
      return;
    }
    System.out.printf("수입날짜 : %s\n", searchedIncome.getDate());
    System.out.printf("수입금액 : %d\n", searchedIncome.getAmount());
    Wallet account = (Wallet) wallet[searchedIncome.getAccount()];
    System.out.printf("수입출처 : %s\n", account.getAssetType());
    System.out.printf("카테고리 : %s\n", searchedIncome.getCategory());
  }

  public void updateIncome() {
    printNoList(PROCESS_UPDATE);
    int incomeNo = Prompt.inputInt("변경 할 수입 번호?");
    Finance updateIncome = (Finance) incomeList.get(incomeList.indexOf(new Finance(incomeNo)));
    if (updateIncome == null) {
      System.out.println("없는 수입 번호입니다.");
      return;
    }
    updateIncome.setDate(Prompt.inputDate("수입일(%s)?", updateIncome.getDate()));
    updateIncome.setAmount(Prompt.inputInt("수입금액(%s)", updateIncome.getAmount()));
    setWallet(updateIncome);
    updateIncome.setCategory(Prompt.input("카테고리(%s)", updateIncome.getCategory()));
    System.out.println("변경 완료했습니다.");
  }

  private void deleteIncome() {
    printNoList(PROCESS_DELETE);
    int incomeNo = Prompt.inputInt("삭제 할 수입 번호?");
    Finance deletedIncome = (Finance) incomeList.get(incomeList.indexOf(new Finance(incomeNo)));
    if (deletedIncome != null) {
      incomeList.remove(incomeList.indexOf(deletedIncome));
      System.out.println("삭제 완료했습니다.");
    } else {
      System.out.println("없는 수입 번호입니다.");
    }
  }

  private void printNoList(int processNo) {
    for (Object object : incomeList.toArray()) {
      Finance income = (Finance) object;
      switch (processNo) {
        case PROCESS_UPDATE:
        case PROCESS_DELETE:
        case PROCESS_SEARCH:
          System.out.printf("%d. ", income.getNo());
        case PROCESS_LIST:
          System.out.printf("%s %s %s\n", income.getDate(), income.getCategory(),
              income.getAmount());
          break;
        default:
      }
    }
  }

  private void setWallet(Finance outcome) {
    for (int i = 0; i < wallet.length; i++) {
      Wallet value = (Wallet) wallet[i];
      if (value == null || i == wallet.length - 1) {
        continue;
      }
      System.out.printf("%d. %s\n", i + 1, value.getAssetType());
    }

    while (true) {
      int no = Prompt.inputInt("수입방법?");
      if ((no < 0 || no >= wallet.length) || wallet[no - 1] == null || no == 3) {
        System.out.println("유효한 수입방법이 아닙니다.");
      } else {
        outcome.setAccount(no - 1);
        break;
      }
    }
  }

  public ArrayList getIncomeList() {
    return incomeList;
  }

}
