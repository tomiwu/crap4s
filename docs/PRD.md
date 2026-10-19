# PRD: scala-crap — CRAP score dla Scali 3

## 1. Cel
Narzędzie open source (CLI na Scala CLI), które dla projektu w Scali 3 liczy złożoność cyklomatyczną (CC), pokrycie testami i CRAP score Uncle Boba, a potem wskazuje ryzykowne metody i lambdy.

## 2. Użytkownicy
- Programista Scali, który lokalnie szuka ryzykownych metod.
- Pipeline CI jako bramka jakości: kod wyjścia różny od 0 po przekroczeniu progu.

## 3. Zakres
**W zakresie:**
- Samodzielne CLI uruchamiane przez Scala CLI.
- Tylko Scala 3. Analiza skompilowanego TASTy przez tasty-query (`ch.epfl.scala::tasty-query:1.10.0`).
- Pokrycie z wbudowanej opcji kompilatora Scali 3 `-coverage-out`.
- Wynik w CSV.

**Poza zakresem (non-goals):**
- Scala 2.13.
- Własna instrumentacja kodu oraz JaCoCo.
- Pluginy sbt/Mill (później, M4).
- HTML (później, M4).
- Licencja: później.

## 4. Wymagania funkcjonalne
1. Wczytuje pliki `.tasty` z podanego katalogu przez tasty-query: pełne drzewa, offsety `SourcePosition`, flagi `isSynthetic` i `isInline`.
2. Classpath buduje jawnie, razem z JRE (`jrt:/`).
3. Wczytuje `scoverage.coverage` i `scoverage.measurements.*` z katalogu `-coverage-out`.
4. Dzieli kod na jednostki, liczy CC, pokrycie i CRAP dla każdej jednostki.
5. Liczy agregaty per plik i per pakiet.
6. Wypisuje CSV posortowany malejąco po `crap`.
7. Kończy się kodem różnym od 0, gdy któraś jednostka przekracza próg.

## 5. Specyfikacja CLI
```
crap --tasty out/classes --coverage out/coverage --threshold 6 [--all]
```
| Opcja | Opis | Domyślnie |
|---|---|---|
| `--tasty` | katalog z plikami `.tasty` | wymagane |
| `--coverage` | katalog `-coverage-out` | wymagane |
| `--threshold` | próg CRAP | 6 |
| `--all` | pokaż wszystkie jednostki, nie tylko te powyżej progu | wyłączone |

Kody wyjścia: `0` gdy nic nie przekracza progu, różny od 0 gdy coś przekracza. Szczegółowe kody błędów: *Do ustalenia*. Składnia podawania classpath: *Do ustalenia*.

## 6. Algorytm
### 6.1 Jednostki
- Jednostką jest metoda (`def`) oraz lambda najwyższego poziomu w ciele metody.
- Zagnieżdżone lambdy i lokalne `def` wliczają się do rodzica.
- Lambdy o CC = 1 wliczają się do rodzica.

### 6.2 Złożoność cyklomatyczna (start = 1)
+1 za każdy z elementów:
- `if`
- każdy `case` w `match` i `catch` poza pierwszym
- `while`
- `&&` i `||` (rozpoznawane po symbolu, bo to zwykłe wywołania)
- guard w `case` i w `for` (w TASTy guard w `for` to już `withFilter`). Sam `for` bez guardów nie dodaje nic.

Pomijamy kod syntetyczny (`isSynthetic`). Rozwinięć `inline` (`Inlined`) nie liczymy w miejscu wywołania. Liczymy tylko samą definicję `inline def`.

### 6.3 Pokrycie
- Instrukcje z `scoverage.coverage` przypisujemy do jednostek po zakresach offsetów `[start, end)` w źródle.
- Instrukcja wykonana = jej id występuje w `scoverage.measurements.*`.
- `cov` = wykonane instrukcje / wszystkie instrukcje jednostki.
- Przed napisaniem parsera trzeba sprawdzić dokładny format pliku w `scala3/compiler/src/dotty/tools/dotc/coverage/Serializer.scala`.
- Jednostka bez instrukcji w raporcie: *Do ustalenia*.

### 6.4 CRAP
`CRAP = cc² × (1 − cov)³ + cc`. Próg domyślny: 6.

### 6.5 Agregaty
Per plik i per pakiet. Metryka agregatu (max, suma, średnia): *Do ustalenia*.

## 7. Wynik
CSV z nagłówkiem:
```
file,line,unit,cc,cov,crap
```
- Posortowany malejąco po `crap`.
- Domyślnie tylko jednostki powyżej progu. Z `--all` wszystkie jednostki.
- Format nazwy `unit` (np. lambdy) i precyzja liczb: *Do ustalenia*.
- Gdzie i jak wypisywać agregaty: *Do ustalenia*.

## 8. Kamienie milowe
- **M1:** wczytanie TASTy, podział na jednostki, CC i CSV (bez pokrycia).
- **M2:** parser `-coverage-out`, mapowanie offsetów, `cov` i CRAP.
- **M3:** bramka CI (próg, kod wyjścia, `--all`) oraz agregaty per plik i per pakiet.
- **M4:** raport HTML, integracje sbt/Mill.

## 9. Ryzyka
- Pozycje drzew syntetycznych i `Inlined` mogą nie zgadzać się z pozycjami z raportu pokrycia.
- Ręczne budowanie classpath, w tym JRE (`jrt:/`).
- Heurystyki: `&&`/`||` jako wywołania metod oraz guardy w `for` rozwinięte do `withFilter`.
- Powiązanie z wersją formatu TASTy: nowa Scala może wymagać aktualizacji tasty-query.

## 10. Proces
Kolejne iteracje zapisujemy w repozytorium. Narzędzie do procesu: *Do ustalenia*.

## 11. Otwarte pytania (Do ustalenia)
- Licencja.
- Nazwa narzędzia i sposób dystrybucji (skrypt, natywny obraz, Coursier).
- Podawanie classpath zależności projektu.
- Metryka agregatów i ich miejsce w wyniku.
- Obsługa jednostek bez danych o pokryciu.
- Format kolumny `unit` i precyzja liczb.
- Czy kod testów jest wykluczany z analizy.
- Narzędzie do śledzenia iteracji w repo.
