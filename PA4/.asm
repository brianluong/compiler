  0         LOADL        0
  1         CALL         L10
  2         HALT   (0)   
  3  L10:   PUSH         1
  4         LOADL        -1
  5         LOADL        1
  6         CALL         newobj  
  7         STORE        3[LB]
  8         LOAD         3[LB]
  9         CALLI        L11
 10         LOADL        1
 11         CALL         neg     
 12         LOAD         3[LB]
 13         LOADL        0
 14         CALL         fieldref
 15         CALLI        L12
 16         RETURN (0)   0
 17  L11:   LOADA        0[OB]
 18         LOADL        0
 19         LOADA        0[OB]
 20         CALL         fieldupd
 21         RETURN (0)   0
 22  L12:   LOAD         -1[LB]
 23         LOAD         3[LB]
 24         LOADL        27
 25         CALL         add     
 26         CALL         putintnl
 27         RETURN (0)   1
