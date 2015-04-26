  0         LOADL        0
  1         CALL         L10
  2         HALT   (0)   
  3  L10:   PUSH         1
  4         LOADL        -1
  5         LOADL        1
  6         CALL         newobj  
  7         STORE        3[LB]
  8         LOAD         3[LB]
  9         LOADL        0
 10         LOADL        0
 11         CALL         fieldupd
 12         PUSH         1
 13         LOADL        22
 14         STORE        4[LB]
 15         PUSH         1
 16         LOADL        0
 17         JUMPIF (0)   L11
 18         LOAD         3[LB]
 19         CALLI        L14
 20  L11:   JUMPIF (0)   L12
 21         LOAD         3[LB]
 22         CALLI        L14
 23  L12:   STORE        5[LB]
 24         LOAD         3[LB]
 25         LOADL        0
 26         CALL         fieldref
 27         JUMPIF (0)   L13
 28         LOADL        1
 29         CALL         neg     
 30         STORE        4[LB]
 31  L13:   LOAD         4[LB]
 32         CALL         putintnl
 33         RETURN (0)   0
 34  L14:   LOADA        0[OB]
 35         LOADL        0
 36         LOADL        1
 37         CALL         fieldupd
 38         LOADL        1
 39         RETURN (1)   0
