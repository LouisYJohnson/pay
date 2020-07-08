<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
</head>
<body>

<div id="myQrcode"></div>
<#--通过模板渲染将参数拿进来-->
<div id="orderId" hidden>${orderId}</div>
<div id="returnUrl" hidden>${returnUrl}</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/1.5.1/jquery.min.js"></script>
<script src="https://cdn.bootcdn.net/ajax/libs/jquery.qrcode/1.0/jquery.qrcode.min.js"></script>
<script>
    jQuery('#myQrcode').qrcode({
        text: "${codeUrl}"
    });

    $(function () {
        //定时器,不停的请求后端api
        setInterval(function () {
            console.log('开始查询支付状态...')
            $.ajax({    //result就是从该地址查询得到的pojo
                url: '/pay/queryByOrderId',
                data: {
                    'oderId': $('#orderId').text()
                },
                success: function (result) {
                    console.log(result)
                    if (result.platformStatus != null
                        && result.platformStatus === 'SUCCESS') {
                        location.herf = $('#returnUrl').text()
                    }
                },
                error: function (result) {
                    alert(result)
                }
            })
        }, 2000)
    })
</script>

</body>
</html>