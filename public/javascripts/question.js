$(document).ready(function (){

    $("div.hideable_box>div+div").fadeOut(0);

    // click the title
    // div.hideable_box>div
    // hide the box below it
    // div.hideable_box>div+div
    $(".hideable_box>.box_title").click(function (){
      $(this).siblings().fadeToggle(100);
     });

    //$("#latex_box #tags ul li:nth-child(1)").click(function() {   //this will apply to all anchor tags
    $("#latex_box #inputbuttons #button1").click(function() {   //this will apply to all anchor tags
       //$("#latex_box textarea").val($("#latex_box textarea").val()+'$x_1$');
       $("#latex_box textarea").insertAtCaret('x_{1}',false);
    });

    //$("#latex_box #tags ul li:nth-child(2)").click(function() {   //this will apply to all anchor tags
    $("#latex_box #inputbuttons #button2").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\int_{a}^{b} f(x)\\,dx',false);
    });

    $("#latex_box #inputbuttons #button3").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\alpha',false);
    });

    $("#latex_box #inputbuttons #button4").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\frac{d}{dx}',false);
    });

    $("#latex_box #inputbuttons #button5").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('<font color="blue"> blue text </font>',true)
    });

    $("#latex_box #inputbuttons #button6").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\n\\begin{equation}\n f(x) = g(x) + a \n\\end{equation} \n',true)
    });

    $("#latex_box #inputbuttons #button7").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\left(   \\right)',false)
    });

    $("#latex_box button").click(function() {   //this will apply to all anchor tags
       $("#latex_box p").empty();
       $("#latex_box p").append( $("#latex_box textarea").val() );
       MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense

    });


    //$("#questionByCourse").on('change', function(event) {
     //   alert($("#questionByCourse option:selected" ).text());
      //  jsRoutes.controllers.QuestionController.findByCourse($("#questionByCourse option:selected").text()).ajax({

       // success : function (data) {

       //   }
    //    error : function (data) {

  //          }
       // });
    //});

    $("#courseSelect, #termSelect").on('change', function(e){
        var c = $("#courseSelect option:selected").text();
        var t = $("#termSelect option:selected").text();
        console.log(c.concat(" ").concat(t))
        $.ajax({
          type: 'GET',
          url: "/questions/search/year/"+c+"/"+t,
          //url: '@routes.QuestionController.distinctYears()', //TODO: replace URL with play's javascriptRoutes
          data: { course: c, term: t},
          success: function(d){
            var $el = $("#yearSelect")
            var y = $("#yearSelect option:selected").text();
            console.log("selcted" + y);
            console.log("years found: ");
            console.log(d);
            $("#yearSelect option").each(function(){
                $(this).addClass("semiOpagueText");
            })
            $.each(d, function(value,key) {
              $("#yearSelect option[value='"+key+"']").removeClass("semiOpagueText");
            });
            $el.val(d[0]);

            if ($("#yearSelect option[value='"+y+"']").length > 0){
                console.log("slecting" + y);
                $el.val(y);
            }
          }
        });
    })

    $("#yearSelect, #termSelect").on('change', function(e){
        var y = $("#yearSelect option:selected").text();
        var t = $("#termSelect option:selected").text();
        console.log("selected " + y + " " + t)
        $.ajax({
          type: 'GET',
          url: "/questions/search/course/"+y+"/"+t,
          //url: '@routes.QuestionController.distinctCourses()', //TODO: replace URL with play's javascriptRoutes
          data: { year: y, term: t},
          success: function(d){
            var $el = $("#courseSelect");
            var c = $("#courseSelect option:selected").text();
            console.log("selected" + y);
            console.log("courses found: ");
            console.log(d);
            $("#courseSelect option").each(function(){
                $(this).addClass("semiOpagueText");
            })
            $.each(d, function(value,key) {
              $("#courseSelect option[value='"+key+"']").removeClass("semiOpagueText");
            });
            $el.val(d[0]);

            if ($("#courseSelect option[value='"+c+"']").length > 0){
                console.log("selecting" + c);
                $el.val(c);
            }
          }
        });
    })

});

$.fn.insertAtCaret = function (insertion,notMath) {
  return this.each(function(){
    if (document.selection) {
      //IE support
      this.focus();
      sel = document.selection.createRange();
      sel.text = insertion;
      this.focus();
    }else if (this.selectionStart || this.selectionStart == '0') {
      //MOZILLA/NETSCAPE support
      startPos = this.selectionStart;
      endPos = this.selectionEnd;
      scrollTop = this.scrollTop;
      if(notMath){
        this.value = this.value.substring(0, startPos) + insertion + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length;
        this.selectionEnd = startPos + insertion.length;
      } 
      else {
      if (isInMathEnvironment(this.value.substring(0, startPos))) {
        this.value = this.value.substring(0, startPos) + insertion + ' ' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 1;
        this.selectionEnd = startPos + insertion.length + 1;
      }
      else {
        this.value = this.value.substring(0, startPos) + '$' + insertion + '$' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 2;
        this.selectionEnd = startPos + insertion.length + 2;
      }
    }
      this.scrollTop = scrollTop;
    } else {
      this.value += insertion;
      this.focus();
    }
  });
};

function isInMathEnvironment(aString) {
  //Check if the string contains an odd number of '$'s
  var n = aString.split('$').length - 1;
  var bool = (n%2 == 1);

  //Check if the string contains an odd number of '$'s
  var nb = aString.split('\\begin{equation}').length - 1;
  var ne = aString.split('\\end{equation}').length - 1;
  bool = bool | (nb - ne == 1);

  return bool;

};